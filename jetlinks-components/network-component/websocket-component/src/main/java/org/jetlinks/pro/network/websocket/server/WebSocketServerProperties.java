package org.jetlinks.pro.network.websocket.server;

import io.vertx.core.http.HttpServerOptions;
import lombok.*;
import org.jetlinks.pro.network.security.Certificate;
import org.jetlinks.rule.engine.executor.PayloadType;

/**
 * WebSocket服务配置
 *
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebSocketServerProperties {

    /**
     * 服务配置ID
     */
    private String id;

    /**
     * HOST，绑定网卡
     */
    private String host = "0.0.0.0";

    /**
     * 服务端口
     */
    private int port;

    /**
     * 是否开启SSL
     */
    private boolean ssl;
    /**
     * SSL 证书ID
     *
     * @see Certificate#getId()
     * @see org.jetlinks.pro.network.security.CertificateManager
     */
    private String certId;

    /**
     * 处理消息线程数
     */
    @Builder.Default
    private int instance = Math.max(4, Runtime.getRuntime().availableProcessors());

    /**
     * 其他配置
     */
    private HttpServerOptions options;

}
