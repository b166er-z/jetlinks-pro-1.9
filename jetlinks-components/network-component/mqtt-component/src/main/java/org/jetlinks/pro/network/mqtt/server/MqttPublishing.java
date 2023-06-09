package org.jetlinks.pro.network.mqtt.server;

import org.jetlinks.core.message.codec.MqttMessage;

public interface MqttPublishing {

    MqttMessage getMessage();

    void acknowledge();
}
