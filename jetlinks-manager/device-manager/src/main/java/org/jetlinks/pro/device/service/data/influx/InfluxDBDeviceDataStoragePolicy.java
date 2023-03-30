package org.jetlinks.pro.device.service.data.influx;

import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.data.AbstractDeviceDataStoragePolicy;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.pro.elastic.search.utils.TermCommonUtils;
import org.jetlinks.pro.influx.InfluxDBOperations;
import org.jetlinks.pro.influx.term.InfluxDBQueryConditionBuilder;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.pro.utils.TimeUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class InfluxDBDeviceDataStoragePolicy extends AbstractDeviceDataStoragePolicy {

    protected final InfluxDBOperations operations;


    public InfluxDBDeviceDataStoragePolicy(DeviceRegistry registry,
                                           InfluxDBOperations operations,
                                           DeviceDataStorageProperties properties) {
        super(registry, properties);
        this.operations = operations;
    }

    //转换设备属性为时序数据
    @Override
    protected abstract Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId,
                                                                              DeviceMessage message,
                                                                              Map<String, Object> properties);

    @Nonnull
    @Override
    public Mono<ConfigMetadata> getConfigMetadata() {
        return Mono.empty();
    }

    private final Map<String, Map<String, PropertyMetadata>> metricMetadataCache = new ConcurrentHashMap<>();

    protected abstract List<TimeSeriesMetadata> createTimeSeriesMetadata(String productId, DeviceMetadata metadata);

    private Optional<PropertyMetadata> getMetricColumn(String metric, String column) {
        return Optional.ofNullable(metricMetadataCache.get(metric))
                       .map(cache -> cache.get(column));
    }

    @Nonnull
    @Override
    public Mono<Void> registerMetadata(@Nonnull String productId, @Nonnull DeviceMetadata deviceMetadata) {
        return reloadMetadata(productId, deviceMetadata);
    }

    @Nonnull
    @Override
    public Mono<Void> reloadMetadata(@Nonnull String productId, @Nonnull DeviceMetadata deviceMetadata) {
        for (TimeSeriesMetadata timeSeriesMetadata : createTimeSeriesMetadata(productId, deviceMetadata)) {
            metricMetadataCache
                .put(timeSeriesMetadata.getMetric().getId(), timeSeriesMetadata
                    .getProperties()
                    .stream()
                    .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity())));
        }
        return Mono.empty();
    }

    //预处理查询条件
    protected List<Term> prepareTerms(String metric, List<Term> terms) {

        if (CollectionUtils.isEmpty(terms)) {
            return terms;
        }
        for (Term term : terms) {
            //适配时间戳字段,查询统一使用timestamp
            if (("timestamp".equals(term.getColumn()) || "time".equals(term.getColumn())) && term.getValue() != null) {
                term.setColumn("time");
                term.setValue(prepareTimestampValue(term.getValue(), term.getTermType()));
            } else {
                this.getMetricColumn(metric, term.getColumn())
                    .ifPresent(meta -> {
                        DataType type = meta.getValueType();
                        if (type instanceof Converter) {
                            term.setValue(((Converter<?>) type).convert(term.getValue()));
                        }
                    });
            }

            term.setTerms(prepareTerms(metric, term.getTerms()));
        }

        return terms;
    }

    protected long convertTimestamp(Object val) {
        if (val instanceof String) {
            return TimeUtils.parseDate(String.valueOf(val)).getTime();
        }
        return CastUtils.castDate(val).getTime();
    }

    protected Object prepareTimestampValue(Object value, String type) {
        List<Object> val = TermCommonUtils.convertToList(value);
        if (val.size() == 1) {
            //时间查询要使用纳秒
            long ts = convertTimestamp(val.get(0)) * (1000_000);
            if (TermType.gt.equals(type)) {
                ts += 999_999;
            } else if (TermType.gte.equals(type) || TermType.lte.equals(type)) {
                ts -= 1;
            }
            return ts;
        }
        if (val.size() == 2) {
            long first = convertTimestamp(val.get(0)) * (1000_000);
            long second = convertTimestamp(val.get(1)) * (1000_000);
            second += 999_999;
            return Arrays.asList(first, second);
        }
        return val.stream()
                  .map(v -> prepareTimestampValue(v, type))
                  .collect(Collectors.toList());
    }

    protected String buildOrderBy(QueryParamEntity param) {

        for (Sort sort : param.getSorts()) {
            if (sort.getName().toLowerCase().contains("time")) {
                return " order by time " + sort.getOrder();
            }
        }
        return " order by time desc";
    }

    protected String buildWhere(String metric, QueryParamEntity param, String... and) {
        StringJoiner joiner = new StringJoiner(" ", "where ", "");

        String sql = InfluxDBQueryConditionBuilder.build(prepareTerms(metric, param.getTerms()));

        if (StringUtils.hasText(sql)) {
            joiner.add(sql);
        }

        if (StringUtils.hasText(sql) && and.length > 0) {
            joiner.add("and");
        }
        for (int i = 0; i < and.length; i++) {
            if (i > 0) {
                joiner.add("and");
            }
            joiner.add(and[i]);
        }

        return joiner.length() == 6 ? "" : joiner.toString();
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                       @Nonnull QueryParamEntity query,
                                                       @Nonnull String... properties) {

        if (properties.length == 0) {
            return queryEachProperties(deviceId, query.doPaging(0, 1));
        }
        Set<String> propertiesSet = new HashSet<>(Arrays.asList(properties));

        return this
            .queryEachProperties(deviceId, query.doPaging(0, 1))
            .filter(property -> propertiesSet.contains(property.getProperty()));
    }

    //转换聚合函数
    protected String getAggFunction(Aggregation agg) {
        switch (agg) {
            case AVG: //influxdb 不支持avg,只支持mean
                return "mean";
            case NONE:
            case TOP:
            case FIRST:
                throw new UnsupportedOperationException("不支持的聚合条件:" + agg);
            default:
                return agg.name().toLowerCase();
        }
    }

    private final Set<String> filedProperty = new HashSet<>(Arrays.asList(
        "id", "content", "value", "formatValue", "objectValue"
    ));

    //判断是否为field类型
    // TODO: 2020/9/11 应该使用key对应的物模型判断??
    protected boolean isFieldValue(String key, Object value) {
        if (filedProperty.contains(key)) {
            return true;
        }
        return value instanceof Collection || value instanceof Map;
    }

    //转换值为influxDB field或者tag
    protected void convertValue(Point.Builder builder, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            builder.addField(key, ((Number) value).doubleValue());
        } else if (value instanceof Number) {
            builder.addField(key, ((Number) value).longValue());
        } else if (value instanceof Date) {
            builder.addField(key, ((Date) value).getTime());
        } else if (isFieldValue(key, value)) {
            builder.addField(key, String.valueOf(value));
        } else {
            builder.tag(key, String.valueOf(value));
        }
    }

    @Override
    protected Mono<Void> doSaveData(String metric, Flux<TimeSeriesData> dataStream) {

        return dataStream
            .map(data -> {
                Point.Builder builder = Point
                    .measurement(metric)
                    .time(createUniqueNanoTime(data.getTimestamp()), TimeUnit.NANOSECONDS);

                data.values().forEach((k, v) -> convertValue(builder, k, v));

                return builder.build();
            })
            .collectList()
            .map(points -> BatchPoints.builder().points(points).build())
            .flatMap(operations::write)
            .then();
    }

    @Override
    protected Mono<Void> doSaveData(String metric, TimeSeriesData data) {
        Point.Builder builder = Point
            .measurement(metric)
            .time(createUniqueNanoTime(data.getTimestamp()), TimeUnit.NANOSECONDS);

        data.values().forEach((k, v) -> convertValue(builder, k, v));

        return operations.write(builder.build());
    }


    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull DeviceDataService.AggregationRequest request,
                                                                @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        return aggregationPropertiesByDevice(getProductAndMetadataByProduct(productId), request, properties);
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                               @Nonnull DeviceDataService.AggregationRequest request,
                                                               @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        request.getFilter().and("deviceId", "eq", deviceId);
        return aggregationPropertiesByDevice(getProductAndMetadataByDevice(deviceId), request, properties);
    }

    protected abstract Flux<AggregationData> aggregationPropertiesByDevice(Mono<Tuple2<DeviceProductOperator, DeviceMetadata>> productAndMetadata,
                                                                           DeviceDataService.AggregationRequest request,
                                                                           DeviceDataService.DevicePropertyAggregation... properties);

    protected String getGroupByTime(Interval interval) {
        return "time(" + interval.toString() + ")";
    }

    @Override
    protected <T> Flux<T> doQuery(String metric,
                                  QueryParamEntity paramEntity,
                                  Function<TimeSeriesData, T> mapper) {
        StringJoiner joiner = new StringJoiner("");
        joiner.add("select * from")
              .add(" \"")
              .add(metric)
              .add("\" ")
              .add(buildWhere(metric, paramEntity))
              .add(buildOrderBy(paramEntity));

        if (paramEntity.isPaging()) {
            joiner.add(" limit ").add(String.valueOf(paramEntity.getPageSize()))
                  .add(" offset ")
                  .add(String.valueOf(paramEntity.getPageSize() * paramEntity.getPageIndex()));
        }
        return operations
            .query(joiner.toString())
            .map(mapper);
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity query,
                                                    Function<TimeSeriesData, T> mapper) {
        return doQueryPager(metric, query, mapper, (v) -> true);
    }


    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity query,
                                                    Function<TimeSeriesData, T> mapper,
                                                    Predicate<TimeSeriesData> filter) {
        String sql = "\"" + metric + "\" " + buildWhere(metric, query);
        String countSql = "select count(id) from " + sql;
        String dataSql = "select * from " + sql + buildOrderBy(query) + " limit " + query.getPageSize() + " offset " + query
            .getPageIndex() * query.getPageSize();

        return Mono.zip(
            operations
                .query(countSql)
                .singleOrEmpty()
                .map(data -> data.getInt("count", 0))
                .defaultIfEmpty(0),
            operations
                .query(dataSql)
                .filter(filter)
                .map(mapper)
                .collectList(),
            (total, data) -> PagerResult.of(total, data, query)
        );
    }
}
