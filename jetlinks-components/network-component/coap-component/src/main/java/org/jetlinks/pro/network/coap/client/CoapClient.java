package org.jetlinks.pro.network.coap.client;

import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.core.message.codec.CoapResponseMessage;
import org.jetlinks.pro.network.Network;
import reactor.core.publisher.Mono;

/**
 * CoAP 客户端网络组件,用于向CoAP服务发送消息
 * @author zhouhao
 * @since 1.0
 */
public interface CoapClient extends Network {

    /**
     * @return CoAP客户端配置信息
     */
    CoapClientProperties getProperties();

    /**
     * 发送CoAP消息并获取返回结果
     * @param request CoAP请求 {@link Request#newPost()}
     * @return CoAP响应结果
     */
    Mono<CoapResponse> publish(Request request);

    /**
     * 发送CoAP消息并获取返回结果
     * @param request 请求消息
     * @return 响应结果
     * @see org.jetlinks.core.message.codec.DefaultCoapMessage
     */
    Mono<CoapResponseMessage> publish(CoapMessage request);

}
