package org.jetlinks.pro.network.manager.debug;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.websocket.server.WebSocketServer;
import org.jetlinks.pro.network.websocket.server.WebSocketServerClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketServerDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public WebSocketServerDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "websocket-server-debug";
    }

    @Override
    public String name() {
        return "WebSocket服务调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/websocket/server/*/_subscribe"
        };
    }

    @Override
    public Flux<WebSocketClientMessage> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];

        return subscribe(id, request);
    }

    @SuppressWarnings("all")
    public Flux<WebSocketClientMessage> subscribe(String id, SubscribeRequest request) {

        String message = request.getString("response").filter(StringUtils::hasText).orElse(null);

        byte[] payload = DebugUtils.stringToBytes(message);

        return Flux.create(sink ->
            sink.onDispose(networkManager
                .<WebSocketServer>getNetwork(DefaultNetworkType.WEB_SOCKET_SERVER, id)
                .flatMap(server ->
                    server
                        .handleConnection()
                        .doOnNext(client -> sink.next(WebSocketClientMessage.of(client)))
                        .flatMap(client -> {
                            client.closeHandler(() -> {
                                sink.next(WebSocketClientMessage.ofDisconnect(client));
                            });
                            return client
                                .receive()
                                .map(msg -> WebSocketClientMessage.of(client, msg))
                                .doOnNext(sink::next)
                                .flatMap(msg -> {
                                    if (payload.length > 0) {
                                        return client.send(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, Unpooled.wrappedBuffer(payload)));
                                    }
                                    return Mono.empty();
                                })
                                .then();
                        })
                        .then()
                )
                .doOnError(sink::error)
                .subscriberContext(sink.currentContext())
                .subscribe()
            ));
    }


    @AllArgsConstructor(staticName = "of")
    @Getter
    @Setter
    public static class WebSocketClientMessage {
        private String type;

        private String typeText;

        private Object data;

        public static WebSocketClientMessage of(WebSocketServerClient client) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", client.getRemoteAddress());
            data.put("uri", client.getUri());

            return WebSocketClientMessage.of("connection", "连接", data);
        }

        public static WebSocketClientMessage ofDisconnect(WebSocketServerClient client) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", client.getRemoteAddress());

            return WebSocketClientMessage.of("disconnection", "断开连接", data);
        }

        public static WebSocketClientMessage of(WebSocketServerClient connection, WebSocketMessage message) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", connection.getRemoteAddress().toString());
            data.put("message", message.print());

            return WebSocketClientMessage.of("publish", "消息", data);
        }


    }
}
