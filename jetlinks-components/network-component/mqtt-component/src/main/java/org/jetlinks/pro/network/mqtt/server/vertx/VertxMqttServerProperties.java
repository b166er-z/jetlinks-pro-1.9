package org.jetlinks.pro.network.mqtt.server.vertx;

import io.vertx.mqtt.MqttServerOptions;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VertxMqttServerProperties {

    private String id;

    //服务实例数量(线程数)
    private int instance = Runtime.getRuntime().availableProcessors();

    private String certId;

    private boolean ssl;

    private MqttServerOptions options;

}
