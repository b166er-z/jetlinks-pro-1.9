package org.jetlinks.pro.network.websocket.client;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.network.NetworkProperties;
import org.jetlinks.pro.network.websocket.server.WebSocketServer;
import org.jetlinks.pro.network.websocket.server.WebSocketServerClient;
import org.jetlinks.pro.network.websocket.server.WebSocketServerProperties;
import org.jetlinks.pro.network.websocket.server.WebSocketServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Slf4j
public class WebSocketServerProviderTest {

    static WebSocketServer webSocketServer;


    @BeforeAll
    static void init() {
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setHost("localhost");
        httpServerOptions.setPort(8811);
        WebSocketServerProperties properties = WebSocketServerProperties.builder()
            .id("test")
            .instance(4)
            .port(8811)
            .host("localhost")
            .options(httpServerOptions)
            .build();

        WebSocketServerProvider provider = new WebSocketServerProvider(Vertx.vertx(), (id) -> Mono.empty());

        webSocketServer = provider.createNetwork(properties);
    }

    @Test
    void test() {

        Vertx.vertx().createHttpClient()
            .webSocket(8811, "localhost", "/", res -> {
                if (res.succeeded()) {
                    res.result().write(Buffer.buffer("hello"), r -> {
                        if (r.succeeded()) {
                            log.info("websocket客户端消息发送成功");
                        } else {
                            log.error("websocket客户端消息发送失败", r.cause());
                        }
                    });
                } else {
                    log.error("websocket客户端创建失败", res.cause());
                }
            });


        webSocketServer.handleConnection()
            .flatMap(WebSocketServerClient::receive)
            .map(payload -> payload.getPayload().toString(StandardCharsets.UTF_8))
            .take(1)
            .as(StepVerifier::create)
            .expectNext("hello")
            .verifyComplete();
    }

}
