package org.jetlinks.pro.tdengine;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import javax.sql.DataSource;
import javax.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ConfigurationProperties(prefix = "tdengine")
public class TDengineProperties {

    @NotBlank
    private String database;

    private Connector connector = Connector.jdbc;

    private RestfulConnector restful = new RestfulConnector();

    private JdbcConnector jdbc = new JdbcConnector();

    enum Connector {
        jdbc, restful
    }

    @Getter
    @Setter
    public static class RestfulConnector {
        @NotBlank
        private String endpoint = "http://localhost:6041/";

        private boolean useSSl;

        private String username;

        private String password;

        private Duration connectionTimeout;

        private Duration socketTimeout;

        private DataSize maxInMemorySize;

        //批量提交大小
        private int bufferSize = 5000;

        //缓冲间隔,此时间间隔内没有收到消息,则直接存储,不进行缓冲
        private int bufferRate = 1000;

        //最大缓冲字节数,默认800KB,由于tdengine最大sql长度限制,不能超过1MB
        private DataSize maxBufferBytes = DataSize.ofKilobytes(800);

        //缓冲超时时间,超过此时间没有达到bufferSize也进行存储
        private Duration bufferTimeout = Duration.ofSeconds(3);

        //最大重试次数
        private int maxRetry = 3;

        //背压最大数量,超过后丢弃数据,防止崩溃
        private int backpressureBufferSize = 16;


        public WebClient createClient() {
            TcpClient tcpClient = TcpClient.create();

            if (!connectionTimeout.isNegative()) {
                tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectionTimeout.toMillis()));
            }

            if (!socketTimeout.isNegative()) {
                tcpClient = tcpClient.doOnConnected(connection -> connection
                    .addHandlerLast(new ReadTimeoutHandler(socketTimeout.toMillis(), TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(socketTimeout.toMillis(), TimeUnit.MILLISECONDS)));
            }

            HttpClient httpClient = HttpClient.from(tcpClient);

            if (useSSl) {
                httpClient = httpClient.secure();
            }

            ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
            return WebClient
                .builder()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer
                    .defaultCodecs()
                    .maxInMemorySize((int) maxInMemorySize.toBytes()))
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(username)) {
                        headers.setBasicAuth(username, password);
                    }
                })
                .clientConnector(connector)
                .baseUrl(endpoint)
                .build();
        }
    }

    @Getter
    @Setter
    public static class JdbcConnector {
        private String url;

        private String username;

        private String password;

        private int minimumIdle = 3;

        private int maximumPoolSize = 32;

        private Duration connectionTimeout = Duration.ofSeconds(10);

        private Duration idleTimeout = Duration.ofSeconds(60);

        private String testQuery = "select server_status()";

        private Duration validationTimeout = Duration.ofSeconds(3);


        public DataSource createDataSource() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(Objects.requireNonNull(url, "tdengine.jdbc.url can not be null"));
            config.setUsername(username);
            config.setPassword(password);
            config.setMinimumIdle(minimumIdle);
            config.setMaximumPoolSize(maximumPoolSize);
            config.setConnectionTimeout(connectionTimeout.toMillis());
            config.setIdleTimeout(idleTimeout.toMillis());
            if (StringUtils.hasText(testQuery)) {
                config.setConnectionTestQuery(testQuery);
            }
            config.setValidationTimeout(validationTimeout.toMillis());

            return new HikariDataSource(config);
        }
    }
}
