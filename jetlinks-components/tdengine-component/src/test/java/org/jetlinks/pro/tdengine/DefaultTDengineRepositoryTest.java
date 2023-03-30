package org.jetlinks.pro.tdengine;

import net.sf.jsqlparser.expression.StringValue;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.FloatType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.jetlinks.pro.timeseries.TimeSeriesMetric;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTDengineRepositoryTest {


    @Test
    void testDDL() {
        TestTDengineOperations operations = new TestTDengineOperations();
        TDengineProperties properties=new TDengineProperties();
        properties.setDatabase("jetlinks");
        DefaultTDengineRepository repository = new DefaultTDengineRepository(
            properties, operations);

        TestTimeSeriesMetadata metadata = new TestTimeSeriesMetadata("test");
        metadata.addProperty("value", DoubleType.GLOBAL);
        metadata.addProperty("temp2", IntType.GLOBAL);
        metadata.addProperty("deviceId", StringType.GLOBAL);

        repository.registerMetadata(metadata, Collections.singleton("deviceId"), true)
            .then(
                operations.sqls.filter(sql -> sql.toLowerCase().startsWith("create")).next()
            )
            .doOnNext(sql -> System.out.println(sql))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        metadata = new TestTimeSeriesMetadata("test");
        metadata.addProperty("value", DoubleType.GLOBAL);
        metadata.addProperty("temp3", IntType.GLOBAL);

        repository
            .registerMetadata(metadata, Collections.singleton("deviceId"), true)
            .then(
                operations.sqls.filter(sql -> sql.toLowerCase().startsWith("alter")).next()
            )
            .doOnNext(sql -> System.out.println(sql))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        Flux.range(0, 10)
            .map(i -> Point.of("test", "test_1").value("value", i).tag("deviceId", "test"))
            .collectList()
            .flatMap(repository::insert)
            .then(
                operations.sqls.filter(sql -> sql.toLowerCase().startsWith("insert")).next()
            )
            .doOnNext(sql -> System.out.println(sql))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    }


    static class TestTimeSeriesMetadata implements TimeSeriesMetadata {

        private final TimeSeriesMetric metric;

        public TestTimeSeriesMetadata(String stable) {
            this.metric = TimeSeriesMetric.of(stable);
        }

        private final List<PropertyMetadata> properties = new ArrayList<>();

        public TestTimeSeriesMetadata addProperty(String id, DataType type) {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId(id);
            property.setName(id);
            property.setValueType(type);
            properties.add(property);
            return this;
        }

        @Override
        public TimeSeriesMetric getMetric() {
            return metric;
        }

        @Override
        public List<PropertyMetadata> getProperties() {
            return properties;
        }
    }

    static class TestTDengineOperations implements TDengineOperations {

        EmitterProcessor<String> sqls = EmitterProcessor.create(false);

        @Override
        public Mono<Void> execute(String sql) {
            sqls.onNext(sql);
            return Mono.empty();
        }

        @Override
        public Flux<Map<String, Object>> query(String sql) {
            sqls.onNext(sql);
            return Flux.empty();
        }
    }

}