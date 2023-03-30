package org.jetlinks.pro.messaging.rabbitmq;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;

@Disabled
class ReactorRabbitMQProducerTest {


    @Test
    void test() {

        RabbitProperties properties = new RabbitProperties();
        properties.setUsername("admin");
        properties.setPassword("jetlinks");

        ReactorRabbitMQProducer producer = new ReactorRabbitMQProducer(properties);
        producer.init();
        ReportPropertyMessage propertyMessage = new ReportPropertyMessage();
        propertyMessage.addHeader("productId", "demo-device");
        propertyMessage.setProperties(Collections.singletonMap("temp", "20"));
        propertyMessage.setDeviceId("demo0");

        Flux.range(0, 100000).<AmqpMessage>map(i ->
            SimpleAmqpMessage.of("test", "", null, Unpooled.wrappedBuffer(("test").getBytes()))
        )
            .flatMap(producer::publish)
            .as(StepVerifier::create)
            .expectComplete()
            .verify()
        ;
        producer.shutdown();

    }

}