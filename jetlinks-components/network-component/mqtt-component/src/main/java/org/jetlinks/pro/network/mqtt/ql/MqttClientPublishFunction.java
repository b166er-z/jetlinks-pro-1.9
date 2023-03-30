package org.jetlinks.pro.network.mqtt.ql;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.mqtt.client.MqttClient;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * <pre>
 *
 *     mqtt.client.publish('clientId','topic','JSON','{}') => true
 *
 * </pre>
 */
@Component
@Slf4j(topic = "system.reactor.ql.mqtt.client.publish")
public class MqttClientPublishFunction extends FunctionMapFeature {

    public MqttClientPublishFunction(NetworkManager networkManager) {
        super("mqtt.client.publish", 4, 3, argStream -> argStream
            .collectList()
            .flatMap(args -> {
                String clientId = String.valueOf(args.get(0));
                String topic = String.valueOf(args.get(1));
                PayloadType type = Optional.ofNullable(args.get(2))
                    .map(v -> String.valueOf(v).toUpperCase())
                    .map(PayloadType::valueOf)
                    .orElse(PayloadType.JSON);
                Object payload = args.get(3);
                return networkManager.<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, clientId)
                    .flatMap(client -> client.publish(SimpleMqttMessage.builder()
                        .payloadType(MessagePayloadType.of(type.name()))
                        .payload(type.write(payload))
                        .topic(topic)
                        .build()))
                    .thenReturn(true);
            }).onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.just(false);
            })
        );
    }
}
