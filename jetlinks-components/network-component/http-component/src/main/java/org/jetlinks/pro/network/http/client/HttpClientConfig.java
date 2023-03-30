package org.jetlinks.pro.network.http.client;

import lombok.*;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.pro.network.security.Certificate;

import java.util.Collections;
import java.util.Map;

/**
 * Http Client配置信息
 *
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpClientConfig {

    /**
     * 配置ID
     */
    private String id;

    /**
     * 请求host,如果请求消息{@link HttpRequestMessage#getUrl()}中没有指定则使用此配置
     */
    @Deprecated
    private String host;

    /**
     * 同host配置
     */
    @Deprecated
    private int port;

    /**
     * 请求根地址,如果请求消息{@link HttpRequestMessage#getUrl()}中没有指定http://则使用此配置
     */
    private String baseUrl;

    /**
     * 默认请求头
     */
    private Map<String, String> httpHeaders;

    /**
     * 是否启用SSL
     */
    private boolean ssl;

    /**
     * SSL证书ID
     *
     * @see Certificate#getId()
     */
    private String certId;

    /**
     * 是否验证远程服务的host
     */
    private boolean verifyHost;

    /**
     * 是否信任所有证书
     */
    private boolean trustAll;

    /**
     * 请求超时时间
     */
    private int requestTimeout = 30000;

    /**
     * 证书信息临时变量
     */
    private transient Certificate certificate;

    public Map<String, String> getHttpHeaders() {
        return nullMapHandle(httpHeaders);
    }

    private Map<String, String> nullMapHandle(Map<String, String> map) {
        return map == null ? Collections.emptyMap() : map;
    }

}
