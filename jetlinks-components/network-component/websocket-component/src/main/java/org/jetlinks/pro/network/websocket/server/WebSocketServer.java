package org.jetlinks.pro.network.websocket.server;

import org.jetlinks.pro.network.ServerNetwork;
import reactor.core.publisher.Flux;

/**
 * WebSocket服务
 *
 * @author zhouhao
 * @since 1.0
 */
public interface WebSocketServer extends ServerNetwork {

    /**
     * 监听WebSocket连接
     * @return WebSocket客户端流
     */
    Flux<WebSocketServerClient> handleConnection();


}
