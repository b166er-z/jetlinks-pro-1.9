package org.jetlinks.pro.messaging.rabbitmq;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface RabbitMQProducer {

    Mono<Void> publish(Publisher<AmqpMessage> amqpMessageStream);

    Mono<Void> publish(AmqpMessage message);


    void shutdown();
}
