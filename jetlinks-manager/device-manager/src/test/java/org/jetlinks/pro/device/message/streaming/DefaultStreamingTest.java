package org.jetlinks.pro.device.message.streaming;

import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Collectors;

class DefaultStreamingTest {


    @Test
    void test() {

        DefaultStreaming streaming = DefaultStreaming
            .create("1","test",
                    msg -> Mono.just(1D),
                    flux -> flux
                        .window(2)
                        .flatMap(window -> {
                            return window.collect(Collectors.summingDouble(Double::doubleValue));
                        })
            );

        streaming.output()
                 .doOnSubscribe(s -> {
                     ReportPropertyMessage message = new ReportPropertyMessage();
                     message.setDeviceId("test");
                     Mono.delay(Duration.ofSeconds(1))
                         .flatMap(i->{
                            return streaming
                                 .compute(message)
                                 .then(
                                     streaming.compute(message)
                                 );
                         })
                         .subscribe();
                 })
                 .cast(ReportPropertyMessage.class)
                 .map(msg -> msg.getProperty("test").orElse(0))
                 .take(1)
                 .as(StepVerifier::create)
                 .expectNext(2D)
                 .verifyComplete();


    }

}