package org.jetlinks.pro.messaging.kafka;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Disabled
@Slf4j
class ReactorKafkaProducerTest {


    @Test
    @SneakyThrows
    void testSend() {

        KafkaProperties properties = new KafkaProperties();
        properties.setClientId("test");
        properties.getProducer()
            .setRetries(3);
        properties.getProducer().setAcks("all");

        properties.getConsumer().setGroupId("test-group");
        properties.getConsumer().setClientId("test-client");

        properties.getConsumer().setAutoOffsetReset("latest");

        ReactorKafkaProducer producer = new ReactorKafkaProducer(properties);
        ReactorKafkaConsumer consumer = new ReactorKafkaConsumer(Collections.singleton("test"), properties);
        List<String> list = new ArrayList<>();

        Disposable disposable = consumer
            .subscribe()
            .map(Message::getPayload)
            .map(buf -> buf.toString(StandardCharsets.UTF_8))
            .doOnNext(list::add)
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe(msg -> log.debug("handle message:{}", msg));

        try {
            ReportPropertyMessage propertyMessage = new ReportPropertyMessage();
            propertyMessage.addHeader("productId", "demo-device");
            propertyMessage.setProperties(Collections.singletonMap("temp", "20"));
            propertyMessage.setDeviceId("demo0");

            Duration duration = producer.send(Flux
                .range(0, 200)
                .map(i -> SimpleMessage.of("test", propertyMessage)))
                .as(StepVerifier::create)
                .expectComplete()
                .verify();
            System.out.println(duration);
        } finally {
            System.out.println(list.size());
        }

        Thread.sleep(2000);
        disposable.dispose();
        System.out.println(list.size());
    }

}