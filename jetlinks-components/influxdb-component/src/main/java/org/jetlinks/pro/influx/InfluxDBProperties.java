package org.jetlinks.pro.influx;

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

import javax.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDBProperties {

    @NotBlank
    private String endpoint = "http://localhost:8086/";

    private boolean useSSl;

    @NotBlank
    private String database;

    private String username;

    private String password;

    private Duration connectionTimeout;

    private Duration socketTimeout;

    private DataSize maxInMemorySize;

    //批量提交大小
    private int bufferSize = 5000;

    //缓冲间隔,此时间间隔内没有收到消息,则直接存储,不进行缓冲
    private int bufferRate = 1000;

    //缓冲超时时间,超过此时间没有达到bufferSize也进行存储
    private Duration bufferTimeout = Duration.ofSeconds(3);

    //最大重试次数
    private int maxRetry = 3;

    //最大缓存数量
    private int maxBufferSize = 10;

    //最大缓冲字节数
    private DataSize maxBufferBytes = DataSize.ofMegabytes(15);

    public WebClient createClient() {
        TcpClient tcpClient = TcpClient.create();

        if (!connectionTimeout.isNegative()) {
            tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectionTimeout.toMillis()));
        }

        if (!socketTimeout.isNegative()) {
            tcpClient = tcpClient.doOnConnected(connection -> connection //
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
            .codecs(clientCodecConfigurer -> {
                clientCodecConfigurer
                    .defaultCodecs()
                    .maxInMemorySize((int) maxInMemorySize.toBytes());
            })
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
