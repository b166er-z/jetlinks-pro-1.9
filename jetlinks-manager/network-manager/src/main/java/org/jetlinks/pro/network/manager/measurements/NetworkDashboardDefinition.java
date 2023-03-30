package org.jetlinks.pro.network.manager.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.dashboard.DashboardDefinition;

@Getter
@AllArgsConstructor
public enum NetworkDashboardDefinition implements DashboardDefinition {
    instance("network","网络信息");

    private String id;

    private String name;
}
