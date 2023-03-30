package org.jetlinks.pro.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
public enum AlarmState implements EnumDict<String> {

    running("运行中"),
    stopped("已停止");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
