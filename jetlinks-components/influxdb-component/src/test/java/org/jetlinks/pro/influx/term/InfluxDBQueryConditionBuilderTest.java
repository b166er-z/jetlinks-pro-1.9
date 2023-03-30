package org.jetlinks.pro.influx.term;

import org.hswebframework.ezorm.core.dsl.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InfluxDBQueryConditionBuilderTest {

    @Test
    void test() {
        String sql = Query.of()
            .where("deviceId", "1234")
            .and("time", "2012-11-01")
            .execute(q -> InfluxDBQueryConditionBuilder.build(q.getTerms()));

        assertEquals(sql, "\"deviceId\" = '1234' and \"time\" = '2012-11-01'");
    }

}