package org.jetlinks.pro.messaging.kafka;

import reactor.core.publisher.Flux;

public interface KafkaConsumer {

    Flux<Message> subscribe();

    void shutdown();

}
