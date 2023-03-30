package org.jetlinks.pro.network.http.server;

import org.jetlinks.pro.network.ServerNetwork;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

/**
 * HTTP 服务网络组件接口
 *
 * @author zhouhao
 * @since 1.0
 */
public interface HttpServer extends ServerNetwork {

    /**
     * 监听所有请求
     *
     * @return HttpExchange
     */
    Flux<HttpExchange> handleRequest();

    /**
     * 根据请求方法和url监听请求.
     * <p>
     * URL支持通配符:
     * <pre>
     *   /device/* 匹配/device/下1级的请求,如: /device/1
     *
     *   /device/** 匹配/device/下N级的请求,如: /device/1/2/3
     *
     * </pre>
     *
     * @param method     请求方法: {@link org.springframework.http.HttpMethod}
     * @param urlPattern url
     * @return HttpExchange
     */
    Flux<HttpExchange> handleRequest(String method, String... urlPattern);

    /**
     * 停止服务
     */
    void shutdown();
}
