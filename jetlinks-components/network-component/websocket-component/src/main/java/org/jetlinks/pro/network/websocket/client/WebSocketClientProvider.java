package org.jetlinks.pro.network.websocket.client;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.network.*;
import org.jetlinks.pro.network.security.CertificateManager;
import org.jetlinks.pro.network.security.VertxKeyCertTrustOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * WebSocket客户端提供商，提供对WebSocket客户端支持
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
public class WebSocketClientProvider implements NetworkProvider<WebSocketProperties> {

    private final Vertx vertx;

    private final CertificateManager certificateManager;

    public WebSocketClientProvider(Vertx vertx, CertificateManager certificateManager) {
        this.vertx = vertx;
        this.certificateManager = certificateManager;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_CLIENT;
    }

    @Nonnull
    @Override
    public VertxWebSocketClient createNetwork(@Nonnull WebSocketProperties properties) {
        VertxWebSocketClient socketClient = new VertxWebSocketClient(properties.getId());

        initWebSocket(socketClient, properties);

        return socketClient;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull WebSocketProperties properties) {
        network.shutdown();
        initWebSocket(((VertxWebSocketClient) network), properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("payloadType", "payloadType", "", new ObjectType())
            .add("certId", "证书id", "", new StringType())
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port", "请求服务端口", "", new IntType())
            .add("host", "请求服务主机地址", "", new StringType())
            .add("uri", "请求路径", "", new StringType())
            .add("verifyHost", "是否验证主机", "", new BooleanType())
            .add("enabled", "是否启用", "", new BooleanType());
    }

    private void initWebSocket(VertxWebSocketClient client, WebSocketProperties properties) {
        client.setPayloadType(properties.getPayloadType());
        vertx.createHttpClient(properties.getOptions())
             .webSocket(properties.getPort(), properties.getHost(), properties.getUri(), result -> {
                 if (result.succeeded()) {
                     log.info("websocket client [{}] connected", properties.getId());
                     client.setWebSocket(result.result());
                 } else {
                     log.error("websocket client [{}] connect error", properties.getId(), result.cause());
                     client.monitor.error(result.cause());
                 }
             });
    }

    @Nonnull
    @Override
    public Mono<WebSocketProperties> createConfig(@Nonnull NetworkProperties properties) {

        return Mono.defer(() -> {
            WebSocketProperties config = FastBeanCopier.copy(properties.getConfigurations(), new WebSocketProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new HttpClientOptions());
            }
            if (config.isSsl() && StringUtils.hasText(config.getCertId())) {
                config.getOptions().setSsl(true);
                config.getOptions().setVerifyHost(config.isVerifyHost());
                return certificateManager.getCertificate(config.getCertId())
                                         .map(VertxKeyCertTrustOptions::new)
                                         .doOnNext(config.getOptions()::setTrustOptions)
                                         .doOnNext(config.getOptions()::setKeyCertOptions)
                                         .thenReturn(config);
            }
            return Mono.just(config);
        });

    }
}
