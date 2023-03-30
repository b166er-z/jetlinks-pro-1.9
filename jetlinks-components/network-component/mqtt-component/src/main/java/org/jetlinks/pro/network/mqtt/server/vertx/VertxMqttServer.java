package org.jetlinks.pro.network.mqtt.server.vertx;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import org.jetlinks.pro.network.mqtt.server.MqttConnection;
import org.jetlinks.pro.network.mqtt.server.MqttServer;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Function;

@Slf4j
public class VertxMqttServer implements MqttServer {


    private final EmitterProcessor<MqttConnection> connectionProcessor = EmitterProcessor.create(false);

    private final FluxSink<MqttConnection> sink = connectionProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private Collection<io.vertx.mqtt.MqttServer> mqttServer;

    protected NetMonitor netMonitor;

    private final String id;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bind;

    public VertxMqttServer(String id) {
        this.netMonitor = NetMonitors.getMonitor("mqtt-server", "id", id);
        this.id = id;
    }

    public void setMqttServer(Collection<io.vertx.mqtt.MqttServer> mqttServer) {
        if (this.mqttServer != null && !this.mqttServer.isEmpty()) {
            shutdown();
        }
        this.mqttServer = mqttServer;
        for (io.vertx.mqtt.MqttServer server : this.mqttServer) {
            server
                .exceptionHandler(error -> {
                    netMonitor.error(error);
                    log.error(error.getMessage(), error);
                })
                .endpointHandler(endpoint -> {
                    if (!connectionProcessor.hasDownstreams()) {
                        log.info("mqtt server no handler for:[{}]", endpoint.clientIdentifier());
                        endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                        return;
                    }
                    netMonitor.buffered(connectionProcessor.getPending());
                    if (connectionProcessor.getPending() >= 10240) {
                        log.warn("too many no handle mqtt connection : {}", connectionProcessor.getPending());
                        endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                        return;
                    }
                    sink.next(new VertxMqttConnection(netMonitor, endpoint));
                });
        }
    }


    @Override
    public Flux<MqttConnection> handleConnection() {
        return connectionProcessor;
    }

    @Override
    public boolean isAlive() {
        return mqttServer != null && !mqttServer.isEmpty();
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Override
    public void shutdown() {
        if (mqttServer != null) {
            for (io.vertx.mqtt.MqttServer server : mqttServer) {
                server.close(res -> {
                    if (res.failed()) {
                        log.error(res.cause().getMessage(), res.cause());
                    } else {
                        log.debug("mqtt server [{}] closed", server.actualPort());
                    }
                });
            }
            mqttServer.clear();
        }

    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bind;
    }
}
