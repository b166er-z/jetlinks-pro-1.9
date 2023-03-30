package org.jetlinks.pro.gateway.monitor.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.dashboard.DashboardDefinition;

@AllArgsConstructor
@Getter
public enum  GatewayDashboardDefinition implements DashboardDefinition {
    gatewayMonitor("网关监控")

    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
