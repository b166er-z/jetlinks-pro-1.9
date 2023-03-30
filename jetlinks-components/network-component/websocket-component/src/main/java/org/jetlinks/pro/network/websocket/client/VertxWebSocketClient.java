package org.jetlinks.pro.network.websocket.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 使用Vertx实现Websocket客户端
 *
 * @author bestfeng
 * @since 1.0
 */
public class VertxWebSocketClient implements WebSocketClient {

    WebSocket webSocket;

    @Setter
    @Getter
    PayloadType payloadType = PayloadType.STRING;

    NetMonitor monitor;

    @Getter
    private final String id;

    private final EmitterProcessor<WebSocketMessage> processor = EmitterProcessor.create(false);

    public VertxWebSocketClient(String id) {
        this.id = id;
        this.monitor = NetMonitors.getMonitor("websocket-client", "id", id);

    }

    public void setWebSocket(WebSocket webSocket) {
        if (this.webSocket != null && this.webSocket != webSocket) {
            shutdown();
        }
        this.webSocket = webSocket;
        this.webSocket
            .textMessageHandler(buffer -> {
                if (processor.hasDownstreams()) {
                    processor.onNext(DefaultWebSocketMessage.of(
                        WebSocketMessage.Type.TEXT,
                        Unpooled.wrappedBuffer(buffer.getBytes())));
                }
            })
            .closeHandler((nil) -> monitor.disconnected())
            .exceptionHandler(err -> monitor.error(err));
    }

    @Override
    public Mono<Boolean> publish(WebSocketMessage message) {
        return Mono.create((sink) -> {
            try {
                if (webSocket == null || webSocket.isClosed()) {

                    sink.error(new RuntimeException("websocket closed"));
                    return;
                }
                ByteBuf byteBuf = message.getPayload();
                if (payloadType == PayloadType.BINARY) {
                    webSocket.writeFinalBinaryFrame(Buffer.buffer(byteBuf), result -> {
                        if (result.succeeded()) {
                            sink.success(true);
                        } else {
                            monitor.error(result.cause());
                            sink.error(result.cause());
                        }
                    });
                    return;
                }
                webSocket.writeFinalTextFrame(byteBuf.toString(StandardCharsets.UTF_8), result -> {
                    if (result.succeeded()) {
                        sink.success(true);
                    } else {
                        monitor.error(result.cause());
                        sink.error(result.cause());
                    }
                });
            } catch (Throwable e) {
                monitor.error(e);
                sink.error(e);
            }
        });
    }

    @Override
    public Flux<WebSocketMessage> subscribe() {
        return processor;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_CLIENT;
    }

    @Override
    public void shutdown() {
        try {
            if (null != webSocket && !webSocket.isClosed()) {
                webSocket.close();
            }
            webSocket = null;
        } catch (Exception e) {
            monitor.error(e);
        }
    }

    @Override
    public boolean isAlive() {
        return webSocket != null && !webSocket.isClosed();
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }
}
