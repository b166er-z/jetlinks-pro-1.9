package org.jetlinks.pro.rule.engine.messaging;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static org.jetlinks.core.event.Subscription.*;


/**
 * 通过Websocket订阅规则引擎执行相关记录
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class RuleEngineSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    public RuleEngineSubscriptionProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String id() {
        return "rule-engine";
    }

    @Override
    public String name() {
        return "规则引擎";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/rule-engine/**"};
    }

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {
        String subscriber = request.getId();

        return Flux
            .fromIterable(TopicUtils.expand(request.getTopic()))
            .collectList()
            .map(topics -> of(subscriber,
                              topics.toArray(new String[0]),
                              Feature.local,
                              Feature.broker))
            .flatMapMany(eventBus::subscribe)
            .map(msg -> Message.success(request.getId(), msg.getTopic(), msg.decode(true)));
    }
}
