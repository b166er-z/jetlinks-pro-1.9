package org.jetlinks.pro.notify.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.Provider;

@Getter
@AllArgsConstructor
public enum EmailProvider implements Provider {

    embedded("默认")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
