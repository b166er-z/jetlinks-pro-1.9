package org.jetlinks.pro.network.tcp.client;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
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
import java.time.Duration;

@Component
@Slf4j
public class VertxTcpClientProvider implements NetworkProvider<TcpClientProperties> {

    private final CertificateManager certificateManager;

    private final PayloadParserBuilder payloadParserBuilder;

    private final Vertx vertx;

    public VertxTcpClientProvider(CertificateManager certificateManager, Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        this.payloadParserBuilder = payloadParserBuilder;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_CLIENT;
    }

    @Nonnull
    @Override
    public VertxTcpClient createNetwork(@Nonnull TcpClientProperties properties) {
        VertxTcpClient client = new VertxTcpClient(properties.getId());

        initClient(client, properties);

        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull TcpClientProperties properties) {
        initClient(((VertxTcpClient) network), properties);
    }

    public void initClient(VertxTcpClient client, TcpClientProperties properties) {
        NetClient netClient = vertx.createNetClient(properties.getOptions());
        client.setClient(netClient);
        client.setKeepAliveTimeoutMs(properties.getLong("keepAliveTimeout").orElse(Duration.ofMinutes(10).toMillis()));
        netClient.connect(properties.getPort(), properties.getHost(), result -> {
            if (result.succeeded()) {
                log.debug("connect tcp [{}:{}] success", properties.getHost(), properties.getPort());
                client.setRecordParser(payloadParserBuilder.build(properties.getParserType(),properties));
                client.setSocket(result.result());
            } else {
                log.error("connect tcp [{}:{}] error", properties.getHost(), properties.getPort(),result.cause());
            }
        });
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id","id","" , new StringType())
            .add("enabled", "是否启用", "", new BooleanType())
            .add("certId","证书id","" , new StringType())
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port","请求服务端口","" , new IntType())
            .add("host", "请求服务主机地址", "", new StringType())
            .add("parserType","解析器类型","" , new ObjectType())
            .add("parserConfiguration", "配置解析器", "", new ObjectType());
    }

    @Nonnull
    @Override
    public Mono<TcpClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            TcpClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new TcpClientProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new NetClientOptions());
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
