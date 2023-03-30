package org.jetlinks.pro.network.tcp.server;

import org.jetlinks.pro.network.ServerNetwork;
import org.jetlinks.pro.network.tcp.client.TcpClient;
import reactor.core.publisher.Flux;

/**
 * TCP服务
 *
 * @author zhouhao
 * @version 1.0
 **/
public interface TcpServer extends ServerNetwork {

    /**
     * 订阅客户端连接
     *
     * @return 客户端流
     * @see TcpClient
     */
    Flux<TcpClient> handleConnection();

    /**
     * 关闭服务端
     */
    void shutdown();
}
