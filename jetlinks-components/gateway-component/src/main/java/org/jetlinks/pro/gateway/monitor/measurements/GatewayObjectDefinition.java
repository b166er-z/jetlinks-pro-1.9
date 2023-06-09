package org.jetlinks.pro.gateway.monitor.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.dashboard.ObjectDefinition;

@AllArgsConstructor
@Getter
public enum GatewayObjectDefinition implements ObjectDefinition {
    deviceGateway("设备网关"),
    messageGateway("消息网关"),

    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

}
