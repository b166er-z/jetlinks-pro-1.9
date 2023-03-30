package org.jetlinks.pro.notify.wechat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.Provider;

@Getter
@AllArgsConstructor
public enum WechatProvider implements Provider {
    corpMessage("微信企业消息通知")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

}
