package org.jetlinks.pro.influx;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDBException;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class InfluxDBTemplate implements InfluxDBOperations {

    private final String database;

    private final WebClient webClient;

    //批量提交大小
    @Setter
    private int bufferSize = 5000;

    //缓冲间隔,此时间间隔内没有收到消息,则直接存储,不进行缓冲
    @Setter
    private int bufferRate = 1000;

    //缓冲超时时间,超过此时间没有达到bufferSize也进行存储
    @Setter
    private Duration bufferTimeout = Duration.ofSeconds(3);

    @Setter
    private int maxRetry = 3;

    @Getter
    @Setter
    private int backpressureBufferSize = 10;

    @Getter
    @Setter
    private long maxBufferBytes = DataSize.ofMegabytes(15).toBytes();

    FluxSink<Point> sink;

    public InfluxDBTemplate(String database, WebClient webClient) {
        this.database = database;
        this.webClient = webClient;
    }

    private void init() {
        AtomicLong pointCounter = new AtomicLong();

        FluxUtils
            .bufferRate(
                Flux.<Point>create(sink -> this.sink = sink).map(Point::lineProtocol),
                bufferRate,
                bufferSize,
                bufferTimeout,
                (point, list) -> pointCounter.addAndGet(point.length() * 2L) > maxBufferBytes)
            .doOnNext(ignore -> pointCounter.set(0))
            .onBackpressureBuffer(backpressureBufferSize,
                                  list -> log.warn("无法处理更多InfluxDB写入请求"), BufferOverflowStrategy.DROP_OLDEST)
            .publishOn(Schedulers.boundedElastic(), backpressureBufferSize)
            .flatMap(batch -> Mono
                .create(sink -> {
                            int size = batch.size();
                            String data = String.join("\n", batch);
                            long time = System.currentTimeMillis();
                            this
                                .write(data)
                                .retryWhen(Retry
                                               .backoff(maxRetry, Duration.ofSeconds(1))
                                               .filter(err -> !(err instanceof InfluxDBException)))
                                .doOnSuccess(nil -> log.trace("保存InfluxDB[{}]数据成功,数量:{},耗时:{}ms",
                                                              database,
                                                              size,
                                                              System.currentTimeMillis() - time))
                                .doOnError((err) -> log.error("保存InfluxDB数据失败:", err))
                                .doFinally(s -> sink.success())
                                .subscribe();
                        }
                ))
            .onErrorContinue((err, val) -> log.error("保存InfluxDB数据失败:", err))
            .subscribe();
    }

    public void shutdown() {
        sink.complete();
    }

    public WebClient getClient() {
        return webClient;
    }

    @Override
    public Mono<Void> write(Point point) {
        sink.next(point);
        return Mono.empty();
    }

    public Mono<Void> write(String body) {
        return webClient
            .post()
            .uri(builder -> builder
                .path("/write")
                .queryParam("db", database)
                .build())
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(body)
            .exchange()
            .flatMap(response -> {
                if (response.statusCode().isError()) {
                    return handleError(response);
                }
                return response.releaseBody();
            });
    }

    public <T> Mono<T> handleError(ClientResponse response) {
        Throwable err = InfluxDBException
            .buildExceptionForErrorState(response
                                             .headers()
                                             .asHttpHeaders()
                                             .getFirst("X-Influxdb-Error"));
        return response.releaseBody().then(Mono.error(err));
    }

    @Override
    public Mono<Void> write(BatchPoints points) {
        long t = System.currentTimeMillis();
        int size = points.getPoints().size();
        return write(points.lineProtocol())
            .doOnSuccess(nil -> log.trace("保存InfluxDB[{}]数据成功,数量:{},耗时:{}ms",
                                          database,
                                          size,
                                          System.currentTimeMillis() - t));
    }

    @Override
    public Flux<TimeSeriesData> query(String sql) {
        if (!sql.contains("tz(")) {
            sql = sql + " tz('" + ZoneId.systemDefault().toString() + "')";
        }
        String fSql = sql;
        log.trace("execute influxdb[{}] query: {}", database, fSql);
        return webClient
            .get()
            .uri(builder -> builder
                .path("/query")
                .queryParam("db", database)
                .queryParam("q", fSql)
                .build())
            .exchange()
            .flatMapMany(this::convertQueryResponse);
    }

    private Flux<TimeSeriesData> convertQueryResponse(ClientResponse response) {
        if (response.rawStatusCode() != 200) {
            return this.<TimeSeriesData>handleError(response).flux();
        }
        return response
            .bodyToMono(String.class)
            .flatMapMany(json -> {
                JSONObject jsonObject = JSON.parseObject(json);
                JSONArray results = jsonObject.getJSONArray("results");
                return Flux
                    .fromIterable(results)
                    .cast(JSONObject.class)
                    .flatMapIterable(result -> {
                        if (result.containsKey("error")) {
                            throw InfluxDBException.buildExceptionForErrorState(result.getString("error"));
                        }
                        if (!result.containsKey("series")) {
                            return Collections.emptyList();
                        }
                        return result.getJSONArray("series");
                    })
                    .cast(JSONObject.class)
                    .flatMap(ser -> {
                        JSONArray columns = ser.getJSONArray("columns");
                        JSONObject tags = ser.getJSONObject("tags");
                        return Flux
                            .fromIterable(ser.getJSONArray("values"))
                            .cast(JSONArray.class)
                            .map(value -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                if (null != tags) {
                                    row.putAll(tags);
                                }
                                for (int i = 0, size = columns.size(); i < size; i++) {
                                    row.put(columns.getString(i), value.get(i));
                                }
                                String time = (String) row.get("time");
                                long ts = LocalDateTime
                                    .from(DateTimeFormatter.ISO_DATE_TIME.parse(time))
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli();
                                row.remove("time");
                                row.put("timestamp", ts);
                                return TimeSeriesData.of(ts, row);
                            });

                    });
            });
    }
}
