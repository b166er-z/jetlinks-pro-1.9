package org.jetlinks.pro.network.websocket.client;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.pro.network.security.DefaultCertificate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
class WebSocketClientProviderTest {

    static Vertx vertx = Vertx.vertx();

    @BeforeAll
    static void init() {


    }

    @Test
    @SneakyThrows
    void testTLS(){
        DefaultCertificate serverCert = new DefaultCertificate("test", "test");
        DefaultCertificate clientCert = new DefaultCertificate("test", "test");

        byte[] serverKey = StreamUtils.copyToByteArray(new ClassPathResource("server.pem").getInputStream());
        byte[] clientKey = StreamUtils.copyToByteArray(new ClassPathResource("client.pem").getInputStream());
        byte[] trust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.pem").getInputStream());

        serverCert.initPemKey(Collections.singletonList(serverKey), Collections.singletonList(serverKey))
                .initPemTrust(Collections.singletonList(trust));
        clientCert.initPemKey(Collections.singletonList(clientKey), Collections.singletonList(clientKey))
                .initPemTrust(Collections.singletonList(trust));
    }
    @Test
    void test() {

        vertx.createHttpServer()
                .websocketHandler(socket -> {
                    socket.handler(h -> {
                        log.info("接收到客户端发送的数据：{}", h.toString());
                    });
                    socket.write(Buffer.buffer("testData".getBytes()));
                    socket.close();
                }).listen(8811);

        WebSocketClientProvider provider = new WebSocketClientProvider(vertx, id -> Mono.empty());

        WebSocketProperties properties = new WebSocketProperties();
        properties.setOptions(new HttpClientOptions());
        properties.setHost("127.0.0.1");
        properties.setUri("/");
        properties.setPort(8811);

        WebSocketClient socketClient = provider.createNetwork(properties);

        socketClient.subscribe()
                .map(WebSocketMessage::getPayload)
                .map(buf -> buf.toString(StandardCharsets.UTF_8))
                .take(1)
                .as(StepVerifier::create)
                .expectNext("testData")
                .verifyComplete();

    }
}