package org.jetlinks.pro.device.message;

import lombok.AllArgsConstructor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.device.tenant.TenantDeviceMessageTopicUtils;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.tenant.TenantMember;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Component
@AllArgsConstructor
public class DeviceMessageSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    @Override
    public String id() {
        return "device-message-subscriber";
    }

    @Override
    public String name() {
        return "订阅设备消息";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/device/*/*/**"
        };
    }

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {
        return Flux
            .fromIterable(TopicUtils.expand(request.getTopic()))
            .flatMap(topic -> TenantMember
                .fromAuth(request.getAuthentication())
                .map(member ->
                         TenantDeviceMessageTopicUtils
                             .convertTopic(member, request.getTopic())
                             .flatMap(_topic -> this
                                          .subscribe(request.getId(), _topic, member.getUserId(), request.isShared())
                                 , Integer.MAX_VALUE)
                )
                .defaultIfEmpty(Flux.defer(() -> this
                    .subscribe(request.getId(),
                               topic,
                               request.getAuthentication().getUser().getId(), request.isShared())))
                .flatMapMany(Function.identity()), Integer.MAX_VALUE);
    }

    static final Subscription.Feature[] sharedFeature = {
        Subscription.Feature.local,
        Subscription.Feature.broker,
        Subscription.Feature.shared
    };

    static final Subscription.Feature[] defaultFeature = {
        Subscription.Feature.local,
        Subscription.Feature.broker
    };

    private Flux<Message> subscribe(String id, String topic, String userId, boolean shared) {
        Subscription.Feature[] features = shared ? sharedFeature : defaultFeature;

        return eventBus
            .subscribe(
                Subscription.of(
                    "DeviceMessageSubscriptionProvider:" + userId + id,
                    new String[]{topic},
                    features
                ))
            .map(topicMessage -> Message.success(id, topicMessage.getTopic(), topicMessage.decode()));
    }
}
