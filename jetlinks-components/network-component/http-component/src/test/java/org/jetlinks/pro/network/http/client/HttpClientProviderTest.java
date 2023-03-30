package org.jetlinks.pro.network.http.client;

import io.vertx.core.Vertx;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.pro.network.http.DefaultHttpRequestMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpClientProviderTest {


    @Test
    void test() {
        Vertx vertx = Vertx.vertx();

        vertx.createHttpServer()
                .requestHandler(request -> {
                    request.response()
                            .putHeader("Content-Length","2")
                            .write("ok").end();
                })
                .listen(11223);

        HttpClientProvider provider = new HttpClientProvider(i -> Mono.empty(), vertx);

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setHost("localhost");
        clientConfig.setPort(11223);
        clientConfig.setRequestTimeout(10);
        HttpClient client = provider.createNetwork(clientConfig);

        DefaultHttpRequestMessage message = new DefaultHttpRequestMessage();
        message.setUrl("/");
        message.setMethod(HttpMethod.GET);


        client.request(message)
                .map(HttpResponseMessage::getPayload)
                .map(payload -> payload.toString(StandardCharsets.UTF_8))
                .as(StepVerifier::create)
                .expectNext("ok")
                .verifyComplete();


    }
}