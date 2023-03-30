package org.jetlinks.pro.elastic.search.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.elastic.search.embedded.EmbeddedElasticSearch;
import org.jetlinks.pro.elastic.search.embedded.EmbeddedElasticSearchProperties;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.pro.elastic.search.service.reactive.DefaultReactiveElasticsearchClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.HostProvider;
import org.springframework.data.elasticsearch.client.reactive.RequestCreator;
import org.springframework.data.elasticsearch.client.reactive.WebClientProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.ProxyProvider;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Configuration
@Slf4j
@EnableConfigurationProperties({
    ElasticSearchProperties.class,
    EmbeddedElasticSearchProperties.class,
    ElasticSearchIndexProperties.class})
public class ElasticSearchConfiguration {

    private final ElasticSearchProperties properties;

    private final EmbeddedElasticSearchProperties embeddedProperties;

    public ElasticSearchConfiguration(ElasticSearchProperties properties, EmbeddedElasticSearchProperties embeddedProperties) {
        this.properties = properties;
        this.embeddedProperties = embeddedProperties;
    }

    @Bean
    @SneakyThrows
    public DefaultReactiveElasticsearchClient reactiveElasticsearchClient(ClientConfiguration clientConfiguration) {
        if (embeddedProperties.isEnabled()) {
            log.debug("starting embedded elasticsearch on {}:{}",
                      embeddedProperties.getHost(),
                      embeddedProperties.getPort());

            new EmbeddedElasticSearch(embeddedProperties).start();
        }
        WebClientProvider provider = getWebClientProvider(clientConfiguration);

        HostProvider hostProvider = HostProvider.provider(provider,
                                                          clientConfiguration.getHeadersSupplier(),
                                                          clientConfiguration
                                                              .getEndpoints()
                                                              .toArray(new InetSocketAddress[0]));

        DefaultReactiveElasticsearchClient client =
            new DefaultReactiveElasticsearchClient(hostProvider, new RequestCreator() {
            });

        client.setHeadersSupplier(clientConfiguration.getHeadersSupplier());

        return client;
    }

    private static WebClientProvider getWebClientProvider(ClientConfiguration clientConfiguration) {

        Duration connectTimeout = clientConfiguration.getConnectTimeout();
        Duration soTimeout = clientConfiguration.getSocketTimeout();

        TcpClient tcpClient = TcpClient.create();

        if (!connectTimeout.isNegative()) {
            tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()));
        }

        if (!soTimeout.isNegative()) {
            tcpClient = tcpClient.doOnConnected(connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(soTimeout.toMillis(), TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(soTimeout.toMillis(), TimeUnit.MILLISECONDS)));
        }

        if (clientConfiguration.getProxy().isPresent()) {
            String proxy = clientConfiguration.getProxy().get();
            String[] hostPort = proxy.split(":");

            if (hostPort.length != 2) {
                throw new IllegalArgumentException("invalid proxy configuration " + proxy + ", should be \"host:port\"");
            }
            tcpClient = tcpClient
                .proxy(proxyOptions -> proxyOptions
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(hostPort[0])
                    .port(Integer.parseInt(hostPort[1])));
        }

        String scheme = "http";
        HttpClient httpClient = HttpClient.from(tcpClient);
        if (clientConfiguration.useSsl()) {

            Optional<SSLContext> sslContext = clientConfiguration.getSslContext();

            if (sslContext.isPresent()) {
                httpClient = httpClient
                    .secure(sslContextSpec -> sslContextSpec
                        .sslContext(new JdkSslContext(sslContext.get(),
                                                      true,
                                                      null,
                                                      IdentityCipherSuiteFilter.INSTANCE,
                                                      ApplicationProtocolConfig.DISABLED, ClientAuth.NONE, null, false)));
            } else {
                httpClient = httpClient.secure();
            }

            scheme = "https";
        }

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        WebClientProvider provider = WebClientProvider.create(scheme, connector);

        if (clientConfiguration.getPathPrefix() != null) {
            provider = provider.withPathPrefix(clientConfiguration.getPathPrefix());
        }

        provider = provider.withDefaultHeaders(clientConfiguration.getDefaultHeaders())
                           .withWebClientConfigurer(clientConfiguration.getWebClientConfigurer());
        return provider;
    }

}
