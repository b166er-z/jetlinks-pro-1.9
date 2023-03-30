package org.jetlinks.pro.notify.dingtalk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.Provider;

@Getter
@AllArgsConstructor
public enum DingTalkProvider implements Provider {
    dingTalkMessage("钉钉消息通知")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

}
