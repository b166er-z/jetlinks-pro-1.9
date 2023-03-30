package org.jetlinks.pro.standalone.configuration.mqtt;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttServerOptions;
import org.jetlinks.pro.gateway.external.MessagingManager;
import org.jetlinks.pro.gateway.external.mqtt.MqttAuthenticationHandler;
import org.jetlinks.pro.gateway.external.mqtt.MqttMessagingHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "messaging.mqtt", name = "enabled", havingValue = "true")
public class MqttMessagingHandlerConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "messaging.mqtt")
    public MqttServerOptions mqttServerOptions() {
        return new MqttServerOptions();
    }


    @Bean(initMethod = "startup")
    public MqttMessagingHandler mqttMessagingHandler(Vertx vertx,
                                                     MqttServerOptions serverOptions,
                                                     MessagingManager messagingManager,
                                                     MqttAuthenticationHandler authenticationHandler) {
        return new MqttMessagingHandler(vertx, serverOptions, messagingManager, authenticationHandler);
    }

}
