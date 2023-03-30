package org.jetlinks.pro.network.websocket.client;

import io.vertx.core.http.HttpClientOptions;
import lombok.*;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.rule.engine.executor.PayloadType;

/**
 * WebSocket客户端配置信息
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketProperties {

    /**
     * 配置ID
     */
    private String id;

    /**
     * HOST，如: www.domain.com
     */
    private String host;

    /**
     * URI,如: /socket
     */
    private String uri;

    /**
     * 端口
     */
    private int port;

    /**
     * 是否开启SSL
     */
    private boolean ssl;

    /**
     * SSL证书ID
     */
    private String certId;

    /**
     * 是否验证证书HOST
     */
    private boolean verifyHost;

    /**
     * 其他配置信息
     */
    private HttpClientOptions options;

    /**
     * 数据类型
     */
    private PayloadType payloadType = PayloadType.STRING;

}
