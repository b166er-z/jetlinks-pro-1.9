package org.jetlinks.pro.influx;

import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class InfluxDBTemplateTest {


    @Test
    public void testWrite() {
        InfluxDBTemplate template = new InfluxDBTemplate("jetlinks", WebClient.create("http://localhost:8086/"));

        template.write(Point.measurement("cpu_load")
            .tag("system", "test")
            .addField("load", 1)
            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build())
            .as(StepVerifier::create)
            .verifyComplete();

    }

    @Test
    public void testWriteBatch() {
        InfluxDBTemplate template = new InfluxDBTemplate("jetlinks", WebClient.create("http://localhost:8086/"));
        long now = System.currentTimeMillis();

        Flux.range(0, 5000000)
            .index()
            .map(tp2 -> Point.measurement("cpu_load")
                .tag("system", "test-" + ThreadLocalRandom.current().nextInt(10))
                .addField("load", ThreadLocalRandom.current().nextInt(100))
                .time(now + (tp2.getT1() * 1000), TimeUnit.MILLISECONDS)
                .build())
            .buffer(5000)
            .publishOn(Schedulers.parallel())
            .map(points -> BatchPoints.builder().points(points).build())
            .flatMap(template::write)
            .as(StepVerifier::create)
            .verifyComplete();

//        template.query("select * from cpu_load,cpu_load where time >'2020-09-10 10:10:10' group by system slimit 1")
//            .doOnNext(timeSeriesData -> System.out.println(timeSeriesData.getData()))
//            .then()
//            .as(StepVerifier::create)
//            .verifyComplete();

    }

}