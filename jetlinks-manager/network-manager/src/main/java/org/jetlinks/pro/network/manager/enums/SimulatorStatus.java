package org.jetlinks.pro.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum SimulatorStatus implements EnumDict<String> {
    stop("已停止"),
    running("运行中");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
