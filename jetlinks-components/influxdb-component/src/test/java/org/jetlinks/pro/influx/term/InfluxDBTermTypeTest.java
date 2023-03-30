package org.jetlinks.pro.influx.term;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class InfluxDBTermTypeTest {


    @Test
    void test(){

        assertEquals(InfluxDBTermType.is.build("name",123),"\"name\" = 123");

        assertEquals(InfluxDBTermType.gt.build("name",123),"\"name\" > 123");

        assertEquals(InfluxDBTermType.gte.build("name",123),"\"name\" >= 123");

        assertEquals(InfluxDBTermType.lt.build("name",123),"\"name\" < 123");
        assertEquals(InfluxDBTermType.lte.build("name",123),"\"name\" <= 123");


        assertEquals(InfluxDBTermType.like.build("name","123"),"\"name\" = '123'");

        assertEquals(InfluxDBTermType.like.build("name","%123"),"\"name\" = ~/^123/");
        assertEquals(InfluxDBTermType.like.build("name","%123%"),"\"name\" = ~/123/");
        assertEquals(InfluxDBTermType.like.build("name","123%"),"\"name\" = ~/123$/");

        assertEquals(InfluxDBTermType.btw.build("name",1),"\"name\" >= 1");

        assertEquals(InfluxDBTermType.btw.build("name", Arrays.asList(1,2)),"\"name\" >= 1 and \"name\" <= 2");

        assertEquals(InfluxDBTermType.in.build("name",Arrays.asList(1,2)),"(\"name\" = 1 or \"name\" = 2)");

        assertEquals(InfluxDBTermType.nin.build("name",Arrays.asList(1,2)),"(\"name\" != 1 and \"name\" != 2)");

    }

}