package org.jetlinks.pro.notify.network.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.Values;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.mqtt.client.MqttClient;
import org.jetlinks.pro.notify.AbstractNotifier;
import org.jetlinks.pro.notify.DefaultNotifyType;
import org.jetlinks.pro.notify.NotifyType;
import org.jetlinks.pro.notify.Provider;
import org.jetlinks.pro.notify.network.NetworkNotifyProvider;
import org.jetlinks.pro.notify.template.TemplateManager;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Slf4j
public class MqttNotifier extends AbstractNotifier<MqttNotifyTemplate> {

    private final String id;

    private final String networkId;

    private final NetworkManager networkManager;

    public MqttNotifier(String id,
                        String networkId,
                        NetworkManager networkManager,
                        TemplateManager templateManager) {
        super(templateManager);
        this.networkManager = networkManager;
        this.id = id;
        this.networkId = networkId;
    }

    @Override
    public String getNotifierId() {
        return id;
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.network;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return NetworkNotifyProvider.MQTT_CLIENT;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull MqttNotifyTemplate template,
                           @Nonnull Values context) {

        MqttMessage msg = template.createMessage(context);

        return networkManager
            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, networkId)
            .flatMap(client ->
                client
                    .publish(msg)
                    .doOnEach(ReactiveLogger.onError(error -> log.error("MQTT[{}]通知失败:\n{}", id, msg.print(), error)))
                    .then())
            .subscriberContext(ReactiveLogger.start("notifierId", id))
            ;
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }
}
