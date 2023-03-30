package org.jetlinks.pro.network.coap.client;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.network.CoapEndpoint;
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
import org.jetlinks.core.metadata.types.LongType;
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
 * CoAP Client网络组建提供商,用于对CoAP Client提供支持
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class CoapClientProvider implements NetworkProvider<CoapClientProperties> {

    private final CertificateManager certificateManager;

    public CoapClientProvider(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_CLIENT;
    }

    @Nonnull
    @Override
    public DefaultCoapClient createNetwork(@Nonnull CoapClientProperties properties) {
        return new DefaultCoapClient(createCoapClient(properties), properties);
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull CoapClientProperties properties) {
        DefaultCoapClient coapClient = ((DefaultCoapClient) network);
        coapClient.client = createCoapClient(properties);
        coapClient.properties = properties;
    }

    private org.eclipse.californium.core.CoapClient createCoapClient(CoapClientProperties properties) {
        org.eclipse.californium.core.CoapClient coapClient = new CoapClient();
        coapClient.setEndpoint(new CoapEndpoint.Builder()
                                   .setConnector(createConnector(properties))
                                   .build());

        return coapClient;
    }

    private Connector createConnector(CoapClientProperties properties) {
        if (properties.isEnableDtls()) {
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
                                         .setClientOnly()
                                         .setPskStore(new InMemoryPskStore())
                                         .setIdentity(privateKey, x509KeyManager.getCertificateChain(null),
                                                      CertificateType.RAW_PUBLIC_KEY,
                                                      CertificateType.X_509)
                                         .setTrustCertificateTypes(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY)
                                         .setCertificateVerifier(new StaticCertificateVerifier(certificate.getTrustCerts()))
                                         .setRpkTrustAll()
                                         .build());
        }
        return new UDPConnector();
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("url", "请求服务路径", "", new StringType())
            .add("certId", "证书id", "", new StringType())
            .add("enableDtls", "是否开启dtls", "", new BooleanType())
            .add("timeout", "请求超时时间", "", new LongType())
            .add("retryTimes", "重试次数", "", new IntType())
            .add("privateKeyAlias", "私钥别名", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<CoapClientProperties> createConfig(@Nonnull NetworkProperties properties) {

        return Mono.defer(() -> {
            CoapClientProperties clientProperties = FastBeanCopier.copy(properties.getConfigurations(), new CoapClientProperties());
            clientProperties.setId(properties.getId());
            clientProperties.validate();
            if (clientProperties.isEnableDtls()) {
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
