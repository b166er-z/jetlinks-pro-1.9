package org.jetlinks.pro.messaging.rabbitmq;

import reactor.core.publisher.Flux;

public interface RabbitMQConsumer {

    /**
     * 订阅消息,多次订阅会收到相同的消息.
     *
     * @return 消息流
     */
    Flux<AmqpMessage> subscribe();

    /**
     * 停止消费
     */
    void shutdown();

}
