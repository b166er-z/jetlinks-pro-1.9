package org.jetlinks.pro.network.http.client;

import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.pro.network.Network;
import reactor.core.publisher.Flux;

/**
 * Http 客户端网络组件接口
 *
 * @author zhouhao
 * @since 1.0
 **/
public interface HttpClient extends Network {

    /**
     * 发起请求
     *
     * @param message 请求消息
     * @return 响应流
     */
    Flux<? extends HttpResponseMessage> request(HttpRequestMessage message);
}
