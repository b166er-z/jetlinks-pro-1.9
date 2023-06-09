package org.jetlinks.pro.notify.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum SubscribeState implements EnumDict<String> {
    enabled("订阅中"),
    disabled("已停止");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
