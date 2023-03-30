package org.jetlinks.pro.network.websocket.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketSession;
import org.jetlinks.core.message.codec.http.websocket.WebSocketSessionMessageWrapper;
import org.jetlinks.pro.network.monitor.NetMonitor;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wangzheng
 * @author zhouhao
 * @see WebSocketSession
 * @since 1.0
 */
@Slf4j
public class VertxWebSocketServerClient implements WebSocketServerClient {

    private final ServerWebSocket serverWebSocket;

    private final InetSocketAddress address;

    private final EmitterProcessor<WebSocketMessage> processor = EmitterProcessor.create(false);

    private final FluxSink<WebSocketMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private Duration keepAliveTimeout = Duration.ofMillis(-1);

    private long lastKeepAliveTime = System.currentTimeMillis();

    private final List<Runnable> closeHandler = new CopyOnWriteArrayList<>();

    private final NetMonitor netMonitor;

    @Getter
    private final String id;

    public VertxWebSocketServerClient(ServerWebSocket serverWebSocket, NetMonitor netMonitor) {
        this.serverWebSocket = serverWebSocket;
        doReceived();
        SocketAddress socketAddress = serverWebSocket.remoteAddress();
        address = new InetSocketAddress(socketAddress.host(), socketAddress.port());
        this.id = IDGenerator.UUID.generate();
        this.netMonitor = netMonitor;
    }

    @Override
    public Optional<InetSocketAddress> getRemoteAddress() {
        return Optional.of(address);
    }

    private void doReceived() {
        serverWebSocket
            .textMessageHandler(text -> handle(textMessage(text)))
            .binaryMessageHandler(msg -> handle(binaryMessage(msg.getByteBuf())))
            .pongHandler(buf -> handle(pongMessage(buf.getByteBuf())))
            .closeHandler((nil) -> {
                doClose();
            })
            .exceptionHandler(err -> {
                netMonitor.error(err);
                log.error(err.getMessage(), err);
            })
        ;
    }

    private void doClose() {
        sink.complete();
        for (Runnable runnable : closeHandler) {
            runnable.run();
        }
        closeHandler.clear();
        netMonitor.disconnected();

    }

    private void handle(WebSocketMessage message) {
        this.lastKeepAliveTime = System.currentTimeMillis();
        netMonitor.bytesRead(message.getPayload().writerIndex());
        if (processor.hasDownstreams()) {
            sink.next(WebSocketSessionMessageWrapper.of(message, this));
        } else {
            log.warn("websocket client[{}] session no handler", address);
        }
    }

    @Override
    public String getUri() {
        return serverWebSocket.uri();
    }

    @Override
    @Nonnull
    public List<Header> getHeaders() {
        return serverWebSocket.headers()
                              .entries()
                              .stream()
                              .map(entry -> {
                                  Header header = new Header();
                                  header.setName(entry.getKey());
                                  header.setValue(new String[]{entry.getValue()});
                                  return header;
                              }).collect(Collectors.toList());
    }

    @Override
    public Optional<Header> getHeader(String s) {
        return Optional.ofNullable(serverWebSocket.headers().getAll(s))
                       .map(list -> new Header(s, list.toArray(new String[0])))
            ;
    }

    @Override
    public Mono<Void> close() {
        return Mono.fromRunnable(serverWebSocket::close);
    }

    @Override
    public Mono<Void> close(int i) {
        return Mono.fromRunnable(() -> serverWebSocket.close((short) i));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Optional<Object> getAttribute(String s) {
        return Optional.ofNullable(attributes.get(s));
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributes.put(s, o);
    }

    @Override
    public Flux<WebSocketMessage> receive() {
        return processor;
    }

    @Override
    public Mono<Void> send(WebSocketMessage webSocketMessage) {
        return doWrite(handler -> {
            netMonitor.bytesSent(webSocketMessage.getPayload().writerIndex());
            switch (webSocketMessage.getType()) {
                case TEXT:
                    serverWebSocket.writeTextMessage(webSocketMessage.payloadAsString(), handler);
                    return;
                case BINARY:
                    serverWebSocket.writeBinaryMessage(Buffer.buffer(webSocketMessage.getPayload()), handler);
                    return;
                case PING:
                    serverWebSocket.writePing(Buffer.buffer(webSocketMessage.getPayload()));
                    handler.handle(Future.succeededFuture());
                    return;
            }

            throw new UnsupportedOperationException("unsupported message type" + webSocketMessage.getType());
        });
    }

    protected Mono<Void> doWrite(Consumer<Handler<AsyncResult<Void>>> handler) {
        this.lastKeepAliveTime = System.currentTimeMillis();
        return Mono.<Void>create(sink -> {
            try {
                handler.accept(result -> {
                    if (result.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(result.cause());
                    }
                });
            } catch (Throwable e) {
                sink.error(e);
            }
        }).doOnError(netMonitor::error);
    }

    @Override
    public WebSocketMessage textMessage(String s) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, Unpooled.wrappedBuffer(s.getBytes()));
    }

    @Override
    public WebSocketMessage binaryMessage(ByteBuf byteBuf) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.BINARY, byteBuf);
    }

    @Override
    public WebSocketMessage pingMessage(ByteBuf byteBuf) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.PING, byteBuf);
    }

    @Override
    public WebSocketMessage pongMessage(ByteBuf byteBuf) {
        return DefaultWebSocketMessage.of(WebSocketMessage.Type.PONG, byteBuf);
    }

    @Override
    public boolean isAlive() {
        return !serverWebSocket.isClosed() && (System.currentTimeMillis() - lastKeepAliveTime) > keepAliveTimeout.toMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration duration) {
        this.keepAliveTimeout = duration;
        this.lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void closeHandler(Runnable handler) {
        closeHandler.add(handler);
    }

    @Override
    public long getLastKeepAliveTime() {
        return lastKeepAliveTime;
    }
}
