package org.jetlinks.pro.gateway.external;

import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

public interface SubscriptionProvider extends Ordered {

    String id();

    String name();

    String[] getTopicPattern();

    Flux<?> subscribe(SubscribeRequest request);

    @Override
   default int getOrder(){
        return Integer.MAX_VALUE;
    }
}
