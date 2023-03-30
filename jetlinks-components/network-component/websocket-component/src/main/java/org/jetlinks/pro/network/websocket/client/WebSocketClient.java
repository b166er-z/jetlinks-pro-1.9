package org.jetlinks.pro.network.websocket.client;

import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.pro.network.Network;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebSocket客户端
 *
 * @since 1.0
 */
public interface WebSocketClient extends Network {

    /**
     * 推送WebSocket消息
     *
     * @param message WebSocketMessage
     * @return 是否成功
     */
    Mono<Boolean> publish(WebSocketMessage message);

    /**
     * 订阅消息
     *
     * @return 消息流
     */
    Flux<WebSocketMessage> subscribe();

}
