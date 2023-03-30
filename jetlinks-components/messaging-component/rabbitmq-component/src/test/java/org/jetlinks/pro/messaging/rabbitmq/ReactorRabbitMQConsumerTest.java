package org.jetlinks.pro.messaging.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import reactor.test.StepVerifier;

@Disabled
@Slf4j
class ReactorRabbitMQConsumerTest {


    @Test
    void test() {
        RabbitProperties properties = new RabbitProperties();
        properties.setUsername("admin");
        properties.setPassword("jetlinks");

        ReactorRabbitMQConsumer consumer = new ReactorRabbitMQConsumer("test", true, properties);
        consumer.init();

        consumer
            .subscribe()
            .doOnNext(msg->log.debug(msg.toString()))
            .count()
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

}