package org.jetlinks.pro.notify.network.http;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.Values;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.http.client.HttpClient;
import org.jetlinks.pro.notify.*;
import org.jetlinks.pro.notify.network.NetworkNotifyProvider;
import org.jetlinks.pro.notify.template.TemplateManager;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Slf4j
public class HttpNotifier extends AbstractNotifier<HttpNotifyTemplate> {

    private final String id;

    private final String networkId;

    private final NetworkManager networkManager;

    public HttpNotifier(String id,
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
        return NetworkNotifyProvider.HTTP_CLIENT;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull HttpNotifyTemplate template,
                           @Nonnull Values context) {

        HttpRequestMessage msg = template.createMessage(context);

        return networkManager
            .<HttpClient>getNetwork(DefaultNetworkType.HTTP_CLIENT, networkId)
            .flatMap(client ->
                client
                    .request(msg)
                    .doOnEach(ReactiveLogger.onError(error -> log.error("HTTP[{}]通知失败:\n{}", id, msg.print(), error)))
                    .doOnEach(ReactiveLogger.onNext(response -> {
                        if (response.getStatus() != 200) {
                            String body = response.print();
                            String requestBody = response.print();
//                            log.error("HTTP[{}]通知失败:\n{}\n\n{}", id, requestBody, body);
                            throw new RuntimeException("HTTP通知失败:\n" + requestBody + "\n\n" + body);
                        } else if (log.isDebugEnabled()) {
                            log.debug("HTTP[{}]通知成功:\n{}\n\n{}", id, msg.print(), response.print());
                        }
                    }))
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
