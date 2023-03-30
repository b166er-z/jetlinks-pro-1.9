package org.jetlinks.pro.network.http.server.vertx;

import io.vertx.core.http.HttpServerOptions;
import lombok.*;
import org.jetlinks.pro.network.security.Certificate;

import java.util.Collections;
import java.util.Map;

/**
 * HTTP服务配置
 *
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpServerConfig {

    /**
     * 服务配置ID
     */
    private String id;

    /**
     * 服务实例数量(线程数)
     */
    private int instance = Math.max(4, Runtime.getRuntime().availableProcessors());

    /**
     * 绑定端口
     */
    private int port;

    /**
     * 绑定网卡
     */
    private String host = "0.0.0.0";

    /**
     * 是否开启SSL
     */
    private boolean ssl;

    /**
     * 证书ID
     *
     * @see Certificate#getId()
     * @see org.jetlinks.pro.network.security.CertificateManager
     */
    private String certId;

    /**
     * HTTP服务其他配置
     */
    private HttpServerOptions options;

    /**
     * 固定响应头信息
     */
    private Map<String, String> httpHeaders;

    public Map<String, String> getHttpHeaders() {
        return nullMapHandle(httpHeaders);
    }

    private Map<String, String> nullMapHandle(Map<String, String> map) {
        return map == null ? Collections.emptyMap() : map;
    }
}
