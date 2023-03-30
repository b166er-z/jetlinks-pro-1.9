package org.jetlinks.pro.network.http.client;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.SneakyThrows;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.network.*;
import org.jetlinks.pro.network.security.CertificateManager;
import org.jetlinks.pro.network.security.VertxKeyCertTrustOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;

/**
 * Http Client 服务上,使用Vertx实现HttpClient
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class HttpClientProvider implements NetworkProvider<HttpClientConfig> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public HttpClientProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Nonnull
    @Override
    public VertxHttpClient createNetwork(@Nonnull HttpClientConfig properties) {
        VertxHttpClient client = new VertxHttpClient(properties.getId());
        client.setWebClient(createHttpClient(properties));
        client.setClientConfig(properties);
        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull HttpClientConfig properties) {
        VertxHttpClient client = ((VertxHttpClient) network);
        client.setWebClient(createHttpClient(properties));
        client.setClientConfig(properties);
    }

    protected WebClient createHttpClient(HttpClientConfig config) {
        WebClientOptions options = new WebClientOptions();
        options.setConnectTimeout(config.getRequestTimeout());
        if (config.isSsl()) {
            options.setSsl(config.isSsl());
            options.setVerifyHost(config.isVerifyHost());
            options.setTrustAll(config.isTrustAll());
            if (config.getCertificate() != null) {
                VertxKeyCertTrustOptions keyCertTrustOptions = new VertxKeyCertTrustOptions(config.getCertificate());
                if (!config.isTrustAll()) {
                    options.setTrustOptions(keyCertTrustOptions);
                }
                if (config.getCertificate().getKeyManagerFactory() != null) {
                    options.setKeyCertOptions(keyCertTrustOptions);
                }
            }
        }
        return WebClient.create(vertx, options);

    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("instance", "服务实例数量(线程数)", "", new IntType())
            .add("certId", "证书id", "", new StringType())
            .add("ssl", "是否开启ssl", "", new BooleanType())
            .add("port", "请求服务端口", "", new IntType())
            .add("httpHeaders", "请求头", "", new ObjectType())
            .add("port", "请求服务端口", "", new IntType())
            .add("host", "请求服务主机地址", "", new StringType())
            .add("baseUrl", "请求路径前缀", "", new StringType())
            .add("verifyHost", "是否验证主机", "", new BooleanType())
            .add("trustAll", "是否信任所有", "", new BooleanType())
            .add("requestTimeout", "请求超时时间", "", new IntType());

    }

    @Nonnull
    @Override
    public Mono<HttpClientConfig> createConfig(@Nonnull NetworkProperties properties) {

        return Mono.defer(() -> {
            HttpClientConfig config = FastBeanCopier.copy(properties.getConfigurations(), new HttpClientConfig());
            config.setId(properties.getId());
            setAddress(config);
            if (config.isSsl() && StringUtils.hasText(config.getCertId())) {
                return certificateManager.getCertificate(config.getCertId())
                                         .doOnNext(config::setCertificate)
                                         .thenReturn(config);
            }
            return Mono.just(config);
        });
    }

    @SneakyThrows
    private void setAddress(HttpClientConfig config) {
        if (StringUtils.hasText(config.getBaseUrl()) && !config.getBaseUrl().startsWith("http")) {
            config.setBaseUrl("https://".concat(config.getBaseUrl()));
        }
        if (StringUtils.hasText(config.getBaseUrl())) {
            URL url = new URL(config.getBaseUrl());
            config.setHost(url.getHost());
            config.setPort(url.getPort());
        }
    }
}
