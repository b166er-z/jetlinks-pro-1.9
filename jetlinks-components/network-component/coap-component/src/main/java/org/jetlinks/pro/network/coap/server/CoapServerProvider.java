package org.jetlinks.pro.network.coap.server;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
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
import org.jetlinks.pro.network.monitor.NetMonitors;
import org.jetlinks.pro.network.security.Certificate;
import org.jetlinks.pro.network.security.CertificateManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.X509KeyManager;
import java.net.InetSocketAddress;
import java.security.PrivateKey;

/**
 * CoAP 服务网络组建提供商,提供在网络组建中的CoAP服务支持.
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class CoapServerProvider implements NetworkProvider<CoapServerProperties> {

    private final CertificateManager certificateManager;

    public CoapServerProvider(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Nonnull
    @Override
    public DefaultCoapServer createNetwork(@Nonnull CoapServerProperties properties) {

        DefaultCoapServer defaultCoapServer = new DefaultCoapServer(
            properties.getId(),
            NetMonitors.getMonitor("coap-server", "id", properties.getId())
        );
        initCoapServer(defaultCoapServer, properties);

        return defaultCoapServer;
    }

    private void initCoapServer(DefaultCoapServer server, CoapServerProperties properties) {
        try {
            CoapServer coapServer = new CoapServer() {
                @Override
                protected Resource createRoot() {
                    return server;
                }
            };
            coapServer.addEndpoint(new CoapEndpoint.Builder()
                                       .setConnector(createConnector(properties))
                                       .build());
            server.setBindAddress(new InetSocketAddress(properties.getAddress(), properties.getPort()));
            server.setCoapServer(coapServer);
        } catch (Throwable e) {
            server.setLastError(e.getMessage());
            throw e;
        }
    }

    private Connector createConnector(CoapServerProperties properties) {
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
                                         .setServerOnly(true)
                                         .setAddress(properties.createSocketAddress())
                                         .setPskStore(new InMemoryPskStore())
                                         .setIdentity(privateKey, x509KeyManager.getCertificateChain(null),
                                                      CertificateType.RAW_PUBLIC_KEY,
                                                      CertificateType.X_509)
                                         .setTrustCertificateTypes(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY)
                                         .setCertificateVerifier(new StaticCertificateVerifier(certificate.getTrustCerts()))
                                         .setRpkTrustAll()
                                         .build());
        }
        // TODO: 2019/12/19 更多参数配置
        return new UDPConnector(properties.createSocketAddress());
    }


    @Override
    public void reload(@Nonnull Network network, @Nonnull CoapServerProperties properties) {
        initCoapServer(((DefaultCoapServer) network), properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {

        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("address", "服务地址", "", new StringType())
            .add("certId", "证书id", "", new StringType())
            .add("enableDtls", "是否开启dtls", "", new BooleanType())
            .add("port", "服务端口", "", new IntType())
            .add("privateKeyAlias", "私钥别名", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<CoapServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            CoapServerProperties clientProperties = FastBeanCopier.copy(properties.getConfigurations(), new CoapServerProperties());
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
