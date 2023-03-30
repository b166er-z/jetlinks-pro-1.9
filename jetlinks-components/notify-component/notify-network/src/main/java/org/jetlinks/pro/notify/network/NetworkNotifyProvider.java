package org.jetlinks.pro.notify.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.Provider;

@Getter
@AllArgsConstructor
public enum  NetworkNotifyProvider implements Provider {

    HTTP_CLIENT,
    MQTT_CLIENT
    ;

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getName() {
        return getId();
    }
}
