package org.jetlinks.pro.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum PluginState implements EnumDict<String> {

    running("运行中"),
    stopped("已停止");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
