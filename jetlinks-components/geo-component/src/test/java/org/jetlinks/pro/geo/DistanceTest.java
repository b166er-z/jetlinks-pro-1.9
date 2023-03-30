package org.jetlinks.pro.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistanceTest {

    @Test
    void test(){

        assertEquals(Distance.of("1m").toString(),"1.0m");

        assertEquals(Distance.of("1.5m").toString(),"1.5m");

        assertEquals(Distance.of("1.532km").toString(),"1.532km");
        assertEquals(Distance.of("10km").toString(),"10.0km");

    }
}