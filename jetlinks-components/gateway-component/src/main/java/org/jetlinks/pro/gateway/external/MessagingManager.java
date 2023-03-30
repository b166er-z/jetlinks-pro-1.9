package org.jetlinks.pro.gateway.external;

import reactor.core.publisher.Flux;

public interface MessagingManager {

    Flux<Message> subscribe(SubscribeRequest request);

}
