package org.jetlinks.pro.elastic.search.configuration;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

@ConfigurationProperties(prefix = "elasticsearch.client")
@Getter
@Setter
public class ElasticSearchProperties {

    private String host = "localhost";
    private int port = 9200;

    private int connectionRequestTimeout = 5000;
    private int connectTimeout = 2000;
    private int socketTimeout = 2000;
    private int maxConnTotal = 30;
    private List<String> hosts;
    private String username;
    private String password;

    public HttpHost[] createHosts() {
        if (CollectionUtils.isEmpty(hosts)) {
            return new HttpHost[]{new HttpHost(host, port, "http")};
        }

        return hosts.stream().map(HttpHost::create).toArray(HttpHost[]::new);
    }

    public RequestConfig.Builder applyRequestConfigBuilder(RequestConfig.Builder builder) {

        builder.setConnectTimeout(connectTimeout);
        builder.setConnectionRequestTimeout(connectionRequestTimeout);
        builder.setSocketTimeout(socketTimeout);

        return builder;
    }

    public HttpAsyncClientBuilder applyHttpAsyncClientBuilder(HttpAsyncClientBuilder builder) {
        builder.setMaxConnTotal(maxConnTotal);
        builder.setMaxConnPerRoute(2 * maxConnTotal);
        //设置用户密码
        if (StringUtils.hasText(username)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        return builder;
    }
}
