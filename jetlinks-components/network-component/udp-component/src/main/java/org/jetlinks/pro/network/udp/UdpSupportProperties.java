package org.jetlinks.pro.network.udp;

import lombok.*;
import org.jetlinks.pro.network.security.Certificate;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;

/**
 * UDP支持配置
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdpSupportProperties {

    /**
     * 配置ID
     */
    private String id;

    /**
     * 远程地址，不为空时，表示UDP客户端
     */
    private String remoteAddress;

    /**
     * 远程端口
     */
    private int remotePort;

    /**
     * 本地地址
     */
    private String localAddress = "0.0.0.0";

    /**
     * 本地端口
     */
    private int localPort;


    /**
     * 是否开启DTLS
     */
    private boolean dtls;

    /**
     * DTLS证书ID
     *
     * @see Certificate#getId()
     * @see org.jetlinks.pro.network.security.CertificateManager
     */
    private String certId;

    /**
     * 证书私钥别名
     */
    private String privateKeyAlias;

    /**
     * 最大接收包长度,默认65535
     */
    private int receiverPacketSize = 0xffff;

    /**
     * 证书详情
     */
    private transient Certificate certificate;

    public InetSocketAddress createRemoteAddress() {
        if (StringUtils.hasText(remoteAddress)) {
            return new InetSocketAddress(remoteAddress, remotePort);
        } else {
            return null;
        }
    }

    public InetSocketAddress createLocalAddress() {
        if (StringUtils.hasText(localAddress)) {
            return new InetSocketAddress(localAddress, localPort);
        } else {
            return new InetSocketAddress(localPort);
        }
    }
}
