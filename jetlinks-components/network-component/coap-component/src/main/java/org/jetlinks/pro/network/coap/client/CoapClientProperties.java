package org.jetlinks.pro.network.coap.client;

import lombok.*;
import org.jetlinks.pro.network.security.Certificate;
import org.springframework.util.StringUtils;

/**
 * CoAP客户端配置信息
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoapClientProperties {
    /**
     * 配置的ID
     */
    private String id;

    /**
     * 请求根地址,如果在程序发起请求时指定的时相对地址,则使用此URL作为根地址进行请求
     */
    private String url;

    /**
     * 是否开启DTLS
     */
    private boolean enableDtls;

    /**
     * DTLS证书ID
     *
     * @see Certificate
     * @see org.jetlinks.pro.network.security.CertificateManager
     */
    private String certId;

    /**
     * 请求超时时间,单位毫秒
     */
    private long timeout;

    /**
     * 请求重试次数
     */
    private int retryTimes;

    /**
     * 证书的私钥别名
     */
    private String privateKeyAlias;

    /**
     * 证书信息,临时变量,在初始化时设置
     */
    private transient Certificate certificate;

    public void validate() {
        if (enableDtls) {
            if (StringUtils.isEmpty(certId) && certificate == null) {
                throw new IllegalArgumentException("请配置证书");
            }
        }
    }


}
