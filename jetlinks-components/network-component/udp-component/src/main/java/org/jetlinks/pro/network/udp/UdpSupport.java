package org.jetlinks.pro.network.udp;

import org.jetlinks.pro.network.ServerNetwork;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * UDP支持
 *
 * @author zhouhao
 */
public interface UdpSupport extends ServerNetwork {

    /**
     * 发送UDP消息
     *
     * @param message UDP消息
     * @return void
     */
    Mono<Void> publish(UdpMessage message);

    /**
     * 订阅UDP消息
     *
     * @return UDP消息流
     */
    Flux<UdpMessage> subscribe();

}
