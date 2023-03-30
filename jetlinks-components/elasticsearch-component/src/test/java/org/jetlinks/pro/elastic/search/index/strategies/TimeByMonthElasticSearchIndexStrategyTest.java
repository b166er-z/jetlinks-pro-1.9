package org.jetlinks.pro.elastic.search.index.strategies;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.pro.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexProperties;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class TimeByMonthElasticSearchIndexStrategyTest {


    @Test
    @Ignore
    void test() {

        TimeByMonthElasticSearchIndexStrategy strategy = new TimeByMonthElasticSearchIndexStrategy(null,new ElasticSearchIndexProperties());

        DefaultElasticSearchIndexMetadata metadata = new DefaultElasticSearchIndexMetadata("time-by-month-index");
        {
            SimplePropertyMetadata prop = new SimplePropertyMetadata();
            prop.setValueType(new DateTimeType());
            prop.setId("timestamp");
            metadata.addProperty(prop);
        }

        {
            SimplePropertyMetadata prop = new SimplePropertyMetadata();
            prop.setValueType(new GeoType());
            prop.setId("location");
            metadata.addProperty(prop);
        }

        {
            SimplePropertyMetadata nest = new SimplePropertyMetadata();
            nest.setValueType(new LongType());
            nest.setId("val");

            SimplePropertyMetadata prop = new SimplePropertyMetadata();
            prop.setValueType(new ObjectType().addPropertyMetadata(nest));
            prop.setId("detail");
            metadata.addProperty(prop);
        }

        strategy.putIndex(metadata)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();

        strategy.loadIndexMetadata("time-by-month-index")
            .flatMapIterable(ElasticSearchIndexMetadata::getProperties)
            .doOnNext(property-> System.out.println(property.getId()+" "+(JSON.toJSONString(property.getValueType()))))
            .then()
            .as(StepVerifier::create)
            .expectComplete()
            .verify();


    }
}