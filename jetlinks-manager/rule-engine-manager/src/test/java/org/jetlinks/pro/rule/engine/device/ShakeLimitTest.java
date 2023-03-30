package org.jetlinks.pro.rule.engine.device;

import org.jetlinks.reactor.ql.ReactorQL;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ShakeLimitTest {

    @Test
    void testSql() {
        ShakeLimit limit = new ShakeLimit();
        limit.setEnabled(true);
        limit.setAlarmFirst(true);
        limit.setTime(1);
        limit.setThreshold(2);

        String sql =  limit.wrapReactorQl("select row.this $this from dual", null);// "select t.*,row row from (select this val from dual) t group by _window('1s'),rowinfo(),take(-1) having row.index > 2";//

        System.out.println(sql);

        ReactorQL ql = ReactorQL.builder()
            .sql(sql)
            .build();

        ql.start(Flux
            .interval(Duration.ofMillis(100))
            .take(Duration.ofSeconds(2))
        )
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNextCount(2)
            .verifyComplete();


    }
}