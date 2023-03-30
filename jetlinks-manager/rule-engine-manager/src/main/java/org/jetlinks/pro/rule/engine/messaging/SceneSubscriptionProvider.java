package org.jetlinks.pro.rule.engine.messaging;

import lombok.AllArgsConstructor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@AllArgsConstructor
public class SceneSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    @Override
    public String id() {
        return "rule-scene";
    }

    @Override
    public String name() {
        return "场景联动事件";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/scene/*"};
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String topic = request.getTopic();
//        Map<String, String> vars = TopicUtils.getPathVariables("/scene/{sceneId}", topic);
//        String sceneId = vars.get("sceneId");
//      权限控制
        return eventBus
            .subscribe(Subscription.of(request.getId(), topic, Subscription.Feature.local, Subscription.Feature.broker))
            .map(msg -> Message.success(request.getId(), msg.getTopic(), msg.decode()));
    }
}
