package org.jetlinks.pro;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntervalTest {


    @Test
    void test(){
        assertEquals(Interval.of("1d").toString(),"1d");
        assertEquals(Interval.of("10d").toString(),"10d");

        assertEquals(Interval.of("1.5d").toString(),"1.5d");
    }

}