package org.jetlinks.pro.notify.voice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.Provider;

@Getter
@AllArgsConstructor
public enum VoiceProvider implements Provider {

    aliyun("阿里云")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
