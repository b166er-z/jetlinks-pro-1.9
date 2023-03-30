package org.jetlinks.pro.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

    @Test
    void testDuration() {
        {
            assertEquals(Duration.ofMillis(100), TimeUtils.parse("100"));
        }

        {
            assertEquals(Duration.ofSeconds(10), TimeUtils.parse("10s"));
        }

        {
            assertEquals(Duration.ofSeconds(62), TimeUtils.parse("1m2s"));
        }
    }

    @Test
    void test() {

        {
            Date date = TimeUtils.parseDate("now");

            Assert.assertEquals(LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault())).getDayOfMonth()
                , LocalDateTime.now().getDayOfMonth());
        }

        {
            Date date = TimeUtils.parseDate("now+1d");

            Assert.assertEquals(LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault())).getDayOfMonth()
                , LocalDateTime.now().getDayOfMonth() + 1);
        }
        {
            Date date = TimeUtils.parseDate("now()+1d");

            Assert.assertEquals(LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault())).getDayOfMonth()
                , LocalDateTime.now().getDayOfMonth() + 1);
        }
        {

            Date date = TimeUtils.parseDate("now+1h");
            Assert.assertEquals(LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault()))
                    .getHour()
                , LocalDateTime.now().getHour() + 1);
        }

        {

            Date date = TimeUtils.parseDate("2020-10-01||+1d");
            Assert.assertEquals(LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault())).getDayOfMonth(), 2);
        }

        {

            Date date = TimeUtils.parseDate("2020-10-01");
            Assert.assertEquals(LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault())).getDayOfMonth(), 1);
        }


    }
}