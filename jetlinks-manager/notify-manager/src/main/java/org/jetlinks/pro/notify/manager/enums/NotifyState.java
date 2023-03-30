package org.jetlinks.pro.notify.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum NotifyState implements EnumDict<String> {

    success("成功"),
    retrying("重试中"),
    error("失败"),
    cancel("已取消");

    private String text;

    @Override
    public String getValue() {
        return name();
    }
}
