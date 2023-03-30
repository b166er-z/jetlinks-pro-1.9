package org.jetlinks.pro.network.http.server.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用Vertx实现HTTP服务
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
public class HttpServerProvider implements NetworkProvider<HttpServerConfig> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public HttpServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Nonnull
    @Override
    public VertxHttpServer createNetwork(@Nonnull HttpServerConfig config) {
        VertxHttpServer server = new VertxHttpServer(config);
        initServer(server, config);
        return server;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull HttpServerConfig config) {
        VertxHttpServer server = ((VertxHttpServer) network);
        initServer(server, config);
    }

    protected HttpServer createHttpServer(HttpServerConfig config) {
        return vertx.createHttpServer(config.getOptions());
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("instance", "服务实例数量(线程数)", "", new IntType())
            .add("certId", "证书id", "", new StringType())
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port", "请求服务端口", "", new IntType())
            .add("httpHeaders", "请求头", "", new ObjectType());
    }

    @Nonnull
    @Override
    public Mono<HttpServerConfig> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            HttpServerConfig config = FastBeanCopier.copy(properties.getConfigurations(), new HttpServerConfig());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new HttpServerOptions());
            }
            config.getOptions().setHost(config.getHost());
            config.getOptions().setPort(config.getPort());

            if (config.isSsl() && StringUtils.hasText(config.getCertId())) {
                config.getOptions().setSsl(true);
                return certificateManager
                    .getCertificate(config.getCertId())
                    .map(VertxKeyCertTrustOptions::new)
                    .doOnNext(config.getOptions()::setKeyCertOptions)
                    .doOnNext(config.getOptions()::setTrustOptions)
                    .thenReturn(config);
            }
            return Mono.just(config);
        });
    }

    private void initServer(VertxHttpServer server, HttpServerConfig config) {
        List<HttpServer> instances = new ArrayList<>(config.getInstance());
        //利用多线程处理请求
        for (int i = 0; i < config.getInstance(); i++) {
            HttpServer httpServer = createHttpServer(config);
            instances.add(httpServer);
        }
        server.setBindAddress(new InetSocketAddress(config.getHost(), config.getPort()));
        server.setHttpServers(instances);
        for (HttpServer httpServer : instances) {
            httpServer.listen(result -> {
                if (result.succeeded()) {
                    log.debug("startup http server on [{}]", server.getBindAddress());
                } else {
                    server.setLastError(result.cause().getMessage());
                    log.warn("startup http server on [{}] failed", server.getBindAddress(), result.cause());
                }
            });
        }

    }
}
