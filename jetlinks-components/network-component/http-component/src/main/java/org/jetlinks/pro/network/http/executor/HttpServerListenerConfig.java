package org.jetlinks.pro.network.http.executor;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.network.Network;
import org.jetlinks.pro.network.http.server.HttpExchange;
import org.springframework.http.HttpMethod;

/**
 * HTTP 服务监听配置
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
public class HttpServerListenerConfig {

    /**
     * 服务ID,对应网络组件中的配置ID
     *
     * @see org.jetlinks.pro.network.NetworkManager
     * @see Network#getId()
     * @see org.jetlinks.pro.network.http.server.HttpServer
     */
    private String serverId;

    /**
     * 监听地址,如: /device/**
     */
    private String url;

    /**
     * 请求方法
     */
    private HttpMethod method;

    public boolean matched(HttpExchange exchange) {
        String url = exchange.request().getUrl();
        if (method != exchange.request().getMethod()) {
            return false;
        }
        return TopicUtils.match(this.url, url);

    }

    public void validate() {

    }

}
