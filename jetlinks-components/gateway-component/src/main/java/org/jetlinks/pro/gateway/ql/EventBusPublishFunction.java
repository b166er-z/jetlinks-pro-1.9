package org.jetlinks.pro.gateway.ql;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * <pre>
 *
 *    select
 *      --                topic , message
 *      message.publish('/a/b/c',  data)
 *
 *    from dual
 *
 * </pre>
 *
 * @since 1.1
 */
@Component
@Slf4j(topic = "system.reactor.ql.message.publish")
public class EventBusPublishFunction extends FunctionMapFeature {

    public EventBusPublishFunction(EventBus eventBus) {
        super("message.publish", 3, 2, argStream -> argStream
            .collectList()
            .flatMap(args -> {
                String topic = String.valueOf(args.get(0));
                Object payload = args.get(1);

                return eventBus.publish(topic, payload);

            }).onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.just(0L);
            })
        );
    }
}
