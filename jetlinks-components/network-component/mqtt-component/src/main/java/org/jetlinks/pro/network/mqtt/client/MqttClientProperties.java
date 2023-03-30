package org.jetlinks.pro.network.mqtt.client;

import io.vertx.mqtt.MqttClientOptions;
import lombok.Getter;
import lombok.Setter;

/**
 * MQTT Client 配置信息
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
public class MqttClientProperties {
    /**
     * 配置ID
     */
    private String id;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * HOST
     */
    private String host;

    /**
     * PORT
     */
    private int port;

    /**
     * 证书ID
     */
    private String certId;

    /**
     * 其他客户端配置
     */
    private MqttClientOptions options;

    /**
     * SSL
     */
    private boolean ssl;

}
