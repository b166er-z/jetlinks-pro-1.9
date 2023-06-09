package org.jetlinks.pro.dashboard.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.dashboard.ObjectDefinition;

@Getter
@AllArgsConstructor
public enum MonitorObjectDefinition implements ObjectDefinition {

    cpu("CPU"),
    memory("内存");

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
