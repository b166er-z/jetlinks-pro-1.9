package org.jetlinks.pro.notify.manager.subscriber.providers;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.notify.manager.subscriber.Notify;
import org.jetlinks.pro.notify.manager.subscriber.Subscriber;
import org.jetlinks.pro.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.topic.Topics;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@Component
public class DeviceAlarmProvider implements SubscriberProvider {

    private final EventBus eventBus;

    public DeviceAlarmProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "device_alarm";
    }

    @Override
    public String getName() {
        return "设备告警";
    }

    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("productId", "产品ID", "产品ID,支持通配符:*", StringType.GLOBAL)
            .add("deviceId", "设备ID", "设备ID,支持通配符:*", StringType.GLOBAL)
            .add("alarmId", "告警ID", "告警ID,支持通配符:*", StringType.GLOBAL)
            ;
    }

    @Override
    public Mono<Subscriber> createSubscriber(String id, Authentication authentication, Map<String, Object> config) {
        ValueObject configs = ValueObject.of(config);

        String productId = configs.getString("productId").orElse("*");
        String deviceId = configs.getString("deviceId").orElse("*");
        String alarmId = configs.getString("alarmId").orElse("*");
        String userId = authentication.getUser().getId();

        String topic = createTopic(productId, deviceId, alarmId);

        return TenantMember
            .fromAuthAll(authentication)
            .map(member -> {
                if (member.isAdmin()) {
                    //管理员则按租户订阅
                    return Topics.tenant(member.getTenant().getId(), topic);
                } else {
                    return Topics.tenantMember(userId, topic);
                }
            })
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .map(list -> list.toArray(new String[0]))
            .defaultIfEmpty(new String[]{topic})
            .map(topics -> () -> createSubscribe(id, topics));

    }

    private String createTopic(String productId,
                               String deviceId,
                               String alarmId) {
        return String.format("/rule-engine/device/alarm/%s/%s/%s", productId, deviceId, alarmId);
    }

    private Flux<Notify> createSubscribe(String id,
                                         String[] topics) {
        Subscription.Feature[] features = new Subscription.Feature[]{Subscription.Feature.local};
//            RunMode.isCloud() //cloud时,从broker中订阅
//                ? new Subscription.Feature[]{Subscription.Feature.broker, Subscription.Feature.shared}
//                : new Subscription.Feature[]{Subscription.Feature.local};

        return Flux
            .defer(() -> this
                .eventBus
                .subscribe(Subscription.of("device-alarm:" + id, topics, features))
                .map(msg -> {
                    JSONObject json = msg.bodyToJson();
                    return Notify.of(
                        String.format("设备[%s]发生告警:[%s]!", json.getString("deviceName"), json.getString("alarmName")),
                        json.getString("alarmId"),
                        System.currentTimeMillis()
                    );
                }));
    }
}
