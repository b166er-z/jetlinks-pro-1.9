package org.jetlinks.pro.network.mqtt.server.vertx;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
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
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class VertxMqttServerProvider implements NetworkProvider<VertxMqttServerProperties> {

    private final CertificateManager certificateManager;
    private final Vertx vertx;

    public VertxMqttServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Nonnull
    @Override
    public VertxMqttServer createNetwork(@Nonnull VertxMqttServerProperties properties) {
        VertxMqttServer server = new VertxMqttServer(properties.getId());
        initServer(server, properties);
        return server;
    }

    private void initServer(VertxMqttServer server, VertxMqttServerProperties properties) {
        List<MqttServer> instances = new ArrayList<>(properties.getInstance());
        for (int i = 0; i < properties.getInstance(); i++) {
            io.vertx.mqtt.MqttServer mqttServer = io.vertx.mqtt.MqttServer.create(vertx, properties.getOptions());
            instances.add(mqttServer);
        }
        server.setBind(new InetSocketAddress(properties.getOptions().getHost(), properties.getOptions().getPort()));
        server.setMqttServer(instances);
        for (MqttServer instance : instances) {
            instance.listen(result -> {
                if (result.succeeded()) {
                    log.debug("startup mqtt server [{}] on port :{} ", properties.getId(), result.result().actualPort());
                } else {
                    server.netMonitor.error(result.cause());
                    server.setLastError(result.cause().getMessage());
                    log.warn("startup mqtt server [{}] error ", properties.getId(), result.cause());
                }
            });
        }
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull VertxMqttServerProperties properties) {
        log.debug("reload mqtt server[{}]", properties.getId());
        initServer((VertxMqttServer) network, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {

        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("host", "请求服务主机地址", "", new StringType())
            .add("certId", "证书id", "", new StringType())
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port", "请求服务端口", "", new IntType())
            .add("username", "用户名", "", new StringType())
            .add("password", "密码", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<VertxMqttServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            VertxMqttServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new VertxMqttServerProperties());
            config.setId(properties.getId());

            config.setOptions(new JSONObject(properties.getConfigurations()).toJavaObject(MqttServerOptions.class));
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
