package org.jetlinks.pro.network.udp;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.network.*;
import org.jetlinks.pro.network.security.Certificate;
import org.jetlinks.pro.network.security.CertificateManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.X509KeyManager;
import java.security.PrivateKey;

/**
 * UDP支持提供商
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class UdpSupportProvider implements NetworkProvider<UdpSupportProperties> {

    private final CertificateManager certificateManager;

    public UdpSupportProvider(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    private Connector createConnector(UdpSupportProperties properties) {
        if (properties.isDtls()) {
            Certificate certificate = properties.getCertificate();
            X509KeyManager x509KeyManager = certificate.getX509KeyManager(properties.getPrivateKeyAlias());
            if (x509KeyManager == null) {
                throw new IllegalArgumentException("key alias not found");
            }
            PrivateKey privateKey = x509KeyManager.getPrivateKey(null);
            if (privateKey == null) {
                throw new IllegalArgumentException("private key not found");
            }
            return new DTLSConnector(new DtlsConnectorConfig.Builder()
                                         .setAddress(properties.createLocalAddress())
                                         .setPskStore(new InMemoryPskStore())
                                         .setIdentity(privateKey, x509KeyManager.getCertificateChain(null),
                                                      CertificateType.RAW_PUBLIC_KEY,
                                                      CertificateType.X_509)
                                         .setTrustCertificateTypes(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY)
                                         .setCertificateVerifier(new StaticCertificateVerifier(certificate.getTrustCerts()))
                                         .setRpkTrustAll()
                                         .build());
        }
        UDPConnector connector = new UDPConnector(properties.createLocalAddress());
        connector.setReceiverPacketSize(properties.getReceiverPacketSize());
        return connector;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    @Nonnull
    @Override
    public DefaultUdpSupport createNetwork(@Nonnull UdpSupportProperties properties) {
        DefaultUdpSupport udpSupport = new DefaultUdpSupport(properties.getId());

        initUpdSupport(udpSupport, properties);
        return udpSupport;
    }

    private void initUpdSupport(DefaultUdpSupport support, UdpSupportProperties properties) {
        try {
            Connector connector = createConnector(properties);
            support.setConnector(connector);
            support.setAddress(properties.createRemoteAddress());
            support.setBindAddress(properties.createLocalAddress());
        } catch (Throwable e) {
            support.setLastError(e.getMessage());
            throw e;
        }
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull UdpSupportProperties properties) {
        network.shutdown();
        initUpdSupport(((DefaultUdpSupport) network), properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("remoteAddress", "远程地址", "", new StringType())
            .add("certId", "证书id", "", new StringType())
            .add("dtls", "是否开启dtls", "", new BooleanType())
            .add("remotePort", "远程端口", "", new IntType())
            .add("localAddress", "本地地址", "", new StringType())
            .add("localPort", "本地端口", "", new IntType())
            .add("privateKeyAlias", "私钥别名", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<UdpSupportProperties> createConfig(@Nonnull NetworkProperties properties) {

        return Mono.defer(() -> {
            UdpSupportProperties clientProperties = FastBeanCopier.copy(properties.getConfigurations(), new UdpSupportProperties());
            clientProperties.setId(properties.getId());
            if (clientProperties.isDtls()) {
                return certificateManager.getCertificate(clientProperties.getCertId())
                                         .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("证书[" + clientProperties
                                             .getCertId() + "]不存在")))
                                         .doOnNext(clientProperties::setCertificate)
                                         .thenReturn(clientProperties);
            }

            return Mono.just(clientProperties);
        });


    }
}
