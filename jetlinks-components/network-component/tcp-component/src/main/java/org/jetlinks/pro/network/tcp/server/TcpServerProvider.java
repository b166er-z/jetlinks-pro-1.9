package org.jetlinks.pro.network.tcp.server;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Values;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.network.*;
import org.jetlinks.pro.network.security.CertificateManager;
import org.jetlinks.pro.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.pro.network.tcp.parser.PayloadParserBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TcpServerProvider implements NetworkProvider<TcpServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    private final PayloadParserBuilder payloadParserBuilder;

    public TcpServerProvider(CertificateManager certificateManager, Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        this.payloadParserBuilder = payloadParserBuilder;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    @Nonnull
    @Override
    public VertxTcpServer createNetwork(@Nonnull TcpServerProperties properties) {

        VertxTcpServer tcpServer = new VertxTcpServer(properties.getId());
        initTcpServer(tcpServer, properties);

        return tcpServer;
    }

    private void initTcpServer(VertxTcpServer tcpServer, TcpServerProperties properties) {
        int instance = Math.max(2, properties.getInstance());
        List<NetServer> instances = new ArrayList<>(instance);
        for (int i = 0; i < instance; i++) {
            instances.add(vertx.createNetServer(properties.getOptions()));
        }
        payloadParserBuilder.build(properties.getParserType(), properties);
        tcpServer.setParserSupplier(() -> payloadParserBuilder.build(properties.getParserType(), properties));
        tcpServer.setServer(instances);
        tcpServer.setKeepAliveTimeout(properties.getLong("keepAliveTimeout", Duration.ofMinutes(10).toMillis()));
        tcpServer.setBind(new InetSocketAddress(properties.getHost(), properties.getPort()));
        for (NetServer netServer : instances) {
            netServer.listen(properties.createSocketAddress(), result -> {
                if (result.succeeded()) {
                    log.info("tcp server startup on {}", result.result().actualPort());
                } else {
                    tcpServer.setLastError(result.cause().getMessage());
                    tcpServer.monitor.error(result.cause());
                    log.error("startup tcp server error", result.cause());
                }
            });
        }
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull TcpServerProperties properties) {
        VertxTcpServer tcpServer = ((VertxTcpServer) network);
        tcpServer.shutdown();
        initTcpServer(tcpServer, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("certId", "CA证书", "", new StringType().expand("selector", "cert"))
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port", "请求服务端口", "", new IntType())
            .add("host", "请求服务主机地址", "", new StringType())
            .add("parserType", "解析器类型", "", new ObjectType())
            .add("parserConfiguration", "配置解析器", "", new ObjectType());
    }

    @Nonnull
    @Override
    public Mono<TcpServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            TcpServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new TcpServerProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new JSONObject(properties.getConfigurations()).toJavaObject(NetServerOptions.class));
            }
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
