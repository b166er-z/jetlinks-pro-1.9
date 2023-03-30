package org.jetlinks.pro.network.websocket.server;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket服务提供商
 *
 * @author wangzheng
 * @since 1.0
 */
@Component
@Slf4j
public class WebSocketServerProvider implements NetworkProvider<WebSocketServerProperties> {


    private final Vertx vertx;
    private final CertificateManager certificateManager;

    public WebSocketServerProvider(Vertx vertx, CertificateManager certificateManager) {
        this.vertx = vertx;
        this.certificateManager = certificateManager;
    }

    private void initServer(VertxWebSocketServer server, WebSocketServerProperties properties) {
        List<HttpServer> instances = new ArrayList<>(properties.getInstance());
        for (int i = 0; i < properties.getInstance(); i++) {
            HttpServer httpServer = vertx.createHttpServer(properties.getOptions());
            instances.add(httpServer);
        }
        server.setBindAddress(new InetSocketAddress(properties.getHost(), properties.getPort()));
        server.setHttpServers(instances);
        for (HttpServer instance : instances) {
            instance.listen(properties.getPort(), properties.getHost(), res -> {
                if (res.succeeded()) {
                    log.info("startup websocket server [{}] on port: {}", properties.getId(), res
                        .result()
                        .actualPort());
                } else {
                    server.setLastError(res.cause().getMessage());
                    log.warn("startup websocket server [{}] error", properties.getId(), res.cause());
                }
            });
        }
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_SERVER;
    }

    @Nonnull
    @Override
    public VertxWebSocketServer createNetwork(@Nonnull WebSocketServerProperties properties) {
        VertxWebSocketServer server = new VertxWebSocketServer(properties.getId());
        initServer(server, properties);
        return server;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull WebSocketServerProperties properties) {
        log.debug("reload mqtt server[{}]", properties.getId());
        initServer((VertxWebSocketServer) network, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("payloadType", "payloadType", "", new ObjectType())
            .add("certId", "证书id", "", new StringType())
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port", "服务端口", "", new IntType())
            .add("host", "服务主机地址", "", new StringType())
            .add("uri", "请求路径", "", new StringType())
            .add("instance", "服务实例数量(线程数)", "", new IntType());
    }

    @Nonnull
    @Override
    public Mono<WebSocketServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            WebSocketServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new WebSocketServerProperties());
            config.setId(properties.getId());

            config.setOptions(new JSONObject(properties.getConfigurations()).toJavaObject(HttpServerOptions.class));

            if (config.isSsl()) {
                config.getOptions().setSsl(true);
                return certificateManager.getCertificate(config.getCertId())
                                         .map(VertxKeyCertTrustOptions::new)
                                         .doOnNext(config.getOptions()::setKeyCertOptions)
                                         .doOnNext(config.getOptions()::setTrustOptions)
                                         .thenReturn(config);
            }
            return Mono.just(config);
        });
    }
}
