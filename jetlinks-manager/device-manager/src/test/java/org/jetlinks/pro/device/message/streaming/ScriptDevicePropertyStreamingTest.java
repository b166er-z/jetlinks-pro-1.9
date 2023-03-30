package org.jetlinks.pro.device.message.streaming;

import groovy.lang.Binding;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ScriptDevicePropertyStreamingTest {

    @Test
    void test() {

        ScriptDevicePropertyStreaming streaming = ScriptDevicePropertyStreaming.create(" if(temp1==null){ $now(); }");

        Binding binding = new Binding();
        binding.setVariable("test", 2);

        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setProperties(Collections.singletonMap("temp", 1));


        Duration duration = Flux
            .range(0, 100000)
            .flatMap(i -> streaming.compute(message))
            .as(StepVerifier::create)
            .expectNextCount(100000)
            .verifyComplete();

        System.out.println(duration);

    }

}