package org.jetlinks.pro.dashboard;

import org.jetlinks.pro.dashboard.MeasurementParameter;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;

class MeasurementParameterTest {

    @Test
    void test() {

        {
            Duration d = MeasurementParameter.of(Collections.singletonMap("time", "1s"))
                .getDuration("time")
                .orElseThrow(NullPointerException::new);
            Assert.assertEquals(d.getSeconds(),1);
        }

        {
            int d = MeasurementParameter.of(Collections.singletonMap("int", "100"))
                .getInt("int")
                .orElseThrow(NullPointerException::new);
            Assert.assertEquals(d,100);
        }

        {
            boolean d = MeasurementParameter.of(Collections.singletonMap("value", "true"))
                .getBoolean("value")
                .orElseThrow(NullPointerException::new);
            Assert.assertTrue(d);
        }

        {
            Date date = MeasurementParameter.of(Collections.singletonMap("value", "2020-01-01"))
                .getDate("value")
                .orElseThrow(NullPointerException::new);
            Assert.assertNotNull(date);
        }
    }

}