package org.jetlinks.pro.network.manager.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.dashboard.ObjectDefinition;

@Getter
@AllArgsConstructor
public enum NetworkObjectDefinition implements ObjectDefinition {
    traffic("网络流量");
    @Override
    public String getId() {
        return name();
    }

    private String name;
}
