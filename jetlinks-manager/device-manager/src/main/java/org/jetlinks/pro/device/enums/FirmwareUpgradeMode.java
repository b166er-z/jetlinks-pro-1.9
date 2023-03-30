package org.jetlinks.pro.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
public enum FirmwareUpgradeMode implements EnumDict<String> {

    pull("设备拉取"),
    push("平台推送");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
