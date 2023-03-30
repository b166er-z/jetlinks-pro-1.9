package org.jetlinks.pro.notify.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.Provider;

@Getter
@AllArgsConstructor
public enum SmsProvider implements Provider {

    aliyunSms("阿里云短信服务"),
    meduSms("漫道短信服务");

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
