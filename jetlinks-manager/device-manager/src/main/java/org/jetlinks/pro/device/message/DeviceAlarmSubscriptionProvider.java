package org.jetlinks.pro.device.message;

import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.topic.Topics;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.jetlinks.core.event.Subscription.Feature;
import static org.jetlinks.core.event.Subscription.of;


/**
 * 通过Websocket订阅设备告警相关消息
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class DeviceAlarmSubscriptionProvider implements SubscriptionProvider {

    private final EventBus eventBus;

    public DeviceAlarmSubscriptionProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String id() {
        return "device-alarm";
    }

    @Override
    public String name() {
        return "设备告警";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/rule-engine/device/alarm/*/*/*"};
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        Map<String, String> vars = TopicUtils
            .getPathVariables("/rule-engine/device/alarm/{productId}/{deviceId}/{alarmId}",
                              request.getTopic());

        String productId = vars.get("productId");
        String deviceId = vars.get("deviceId");
        String alarmId = vars.get("alarmId");
        String userId = request.getAuthentication().getUser().getId();

        String topic = createTopic(productId, deviceId, alarmId);

        return TenantMember
            .fromAuthAll(request.getAuthentication())
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
            .map(topics -> of("device:alarm:" + userId,
                              topics,
                              Feature.local,
                              Feature.broker))
            .flatMapMany(sub -> eventBus.subscribe(sub, Map.class));

    }

    private String createTopic(String productId,
                               String deviceId,
                               String alarmId) {
        return String.format("/rule-engine/device/alarm/%s/%s/%s", productId, deviceId, alarmId);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
