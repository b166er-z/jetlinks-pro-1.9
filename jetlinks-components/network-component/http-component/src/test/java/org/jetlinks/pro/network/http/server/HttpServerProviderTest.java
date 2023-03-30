package org.jetlinks.pro.network.http.server;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.network.http.server.vertx.HttpServerConfig;
import org.jetlinks.pro.network.http.server.vertx.HttpServerProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class HttpServerProviderTest {

    @Test
    @SneakyThrows
    public void testServer() {
        HttpServerOptions options = new HttpServerOptions();
        HttpServerConfig config = new HttpServerConfig();
        config.setOptions(options);
        config.setPort(8080);
        config.setInstance(2);
        HttpServerProvider provider = new HttpServerProvider(i -> Mono.empty(), Vertx.vertx());
        provider.createNetwork(config)
            .handleRequest()
            .flatMap(change -> {
                HttpResponse response = change.response();
                response.header("Content-Length","2");
                return response.write(Unpooled.copiedBuffer("ok".getBytes()))
                    .flatMap(v -> response.end())
                    .thenReturn(change);
            })
            .doOnNext(change -> {
                log.debug("==========requestParam:{}", JSON.toJSONString(change.request().getRequestParam()));
            })
            .flatMap(httpExchange -> httpExchange.request().getBody())
            .doOnNext(byteBuf -> log.debug("++++++++++++:{}",byteBuf.toString()))
            .subscribe();

        Flux.create(sink-> {
            WebClient.create(Vertx.vertx())
                .request(HttpMethod.POST,8080 ,"127.0.0.1","/")
                .addQueryParam("test","value123")
                .putHeader("Content-type", MediaType.APPLICATION_JSON.toString())
                .send(handle-> {
                    sink.next(handle.result().body().toString());
                });
        })
            .take(1)
            .as(StepVerifier::create)
            .expectNextMatches(s-> s.equals("ok"))
            .verifyComplete();
    }

}
