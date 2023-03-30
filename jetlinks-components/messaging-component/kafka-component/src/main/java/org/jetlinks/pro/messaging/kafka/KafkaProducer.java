package org.jetlinks.pro.messaging.kafka;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;


public interface KafkaProducer {

    Mono<Void> send(Publisher<Message> publisher);

    void shutdown();


}
