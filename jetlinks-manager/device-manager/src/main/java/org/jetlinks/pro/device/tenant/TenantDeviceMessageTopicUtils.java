package org.jetlinks.pro.device.tenant;

import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.tenant.Tenant;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.supports.MultiTenantMember;
import org.jetlinks.pro.tenant.term.MultiAssetsTerm;
import org.jetlinks.pro.topic.Topics;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TenantDeviceMessageTopicUtils {


    public static Flux<String> convertTopic(TenantMember member, String deviceMessageTopic) {
        Map<String, String> vars = TopicUtils.getPathVariables("/device/{productId}/{deviceId}/**", deviceMessageTopic);
        String deviceId = vars.get("deviceId");

        if (!deviceId.equals("*")) {
            //不是订阅全部设备就判断其权限
            return member
                .assetPermission(DeviceAssetType.device, Collections.singleton(deviceId))
                .thenMany(Flux.just(deviceMessageTopic));
        }

        List<TenantMember> tenants = member instanceof MultiTenantMember
            ? ((MultiTenantMember) member).getMembers()
            : Collections.singletonList(member);

        return Flux
            .fromIterable(tenants)
            .map(tenantMember -> {
                if (tenantMember.isAdmin()) {
                    return Topics.tenant(tenantMember.getTenant().getId(), deviceMessageTopic);
                } else {
                    return Topics.tenantMember(tenantMember.getUserId(), deviceMessageTopic);
                }
            });
    }

}
