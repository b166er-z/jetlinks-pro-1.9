package org.jetlinks.pro.network.mqtt.server;

import io.vertx.mqtt.messages.MqttSubscribeMessage;

public interface MqttSubscription {

    MqttSubscribeMessage getMessage();

    void acknowledge();

}
