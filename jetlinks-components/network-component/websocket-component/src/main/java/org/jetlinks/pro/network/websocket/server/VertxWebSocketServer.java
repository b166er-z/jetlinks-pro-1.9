package org.jetlinks.pro.network.websocket.server;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author wangzheng
 * @since 1.0
 */
@Slf4j
public class VertxWebSocketServer implements WebSocketServer {

    private Collection<HttpServer> httpServers;

    private final String id;

    private final NetMonitor netMonitor;

    private final EmitterProcessor<WebSocketServerClient> webSocketProcessor = EmitterProcessor.create(false);

    private final FluxSink<WebSocketServerClient> clientFluxSink = webSocketProcessor.sink();

    @Getter
    @Setter
    private String lastError;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    public VertxWebSocketServer(String id) {
        this.id = id;
        this.netMonitor = NetMonitors.getMonitor("websocket-server", "id", id);
    }

    public void setHttpServers(Collection<HttpServer> httpServers) {
        if (this.httpServers != null && !this.httpServers.isEmpty()) {
            shutdown();
        }
        this.httpServers = httpServers;
        for (HttpServer httpServer : this.httpServers) {
            httpServer
                .exceptionHandler(err -> {
                    log.error(err.getMessage(), err);
                    netMonitor.error(err);
                })
                .webSocketHandler(handler -> {
                    if (log.isDebugEnabled()) {
                        log.debug("handle websocket connection [{}]", handler.remoteAddress());
                    }
                    if (!webSocketProcessor.hasDownstreams()) {
                        log.warn("websocket server[{}] no handler", id);
                        handler.close((short) WebSocketCloseStatus.BAD_GATEWAY.code());
                        return;
                    }
                    clientFluxSink.next(new VertxWebSocketServerClient(handler, netMonitor));
                });
        }
    }

    @Override
    public Flux<WebSocketServerClient> handleConnection() {
        return webSocketProcessor;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_SERVER;
    }

    @Override
    public void shutdown() {
        if (httpServers != null) {
            for (HttpServer httpServer : httpServers) {
                httpServer.close(res -> {
                    if (res.failed()) {
                        log.error(res.cause().getMessage(), res.cause());
                    } else {
                        log.debug("http websocket [{}] closed", httpServer.actualPort());
                    }
                });
            }
        }
        httpServers = null;
    }

    @Override
    public boolean isAlive() {
        return httpServers != null && !httpServers.isEmpty();
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
