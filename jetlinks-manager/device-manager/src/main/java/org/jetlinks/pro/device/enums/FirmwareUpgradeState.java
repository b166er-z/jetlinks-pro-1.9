package org.jetlinks.pro.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
public enum FirmwareUpgradeState implements EnumDict<String> {

    waiting("等待升级"),
    processing("升级中"),
    failed("升级失败"),
    success("升级成功"),
    canceled("已取消");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
