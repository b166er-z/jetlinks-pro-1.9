package org.jetlinks.pro.network.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.impl.StreamingBodyCodec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class EventSourceTest {

    private Vertx vertx = Vertx.vertx();

    @Test
    @SneakyThrows
    public void testConnectEventSource() {

//        HttpServerOptions options = new HttpServerOptions();
//        options.setSsl(false);
//        HttpServer server = vertx.createHttpServer(options);
//        Router router = Router.router(vertx);
//        router.route("/test").handler(routingContext -> {
//            HttpServerResponse response = routingContext.response();
//            response.putHeader("content-type", "text/plain");
//            response.end("GET   Hello World from Vert.x-Web!");
//        });
//        server.requestHandler(router).listen(8080);

        WebClient client = WebClient.create(vertx);
        client.request(HttpMethod.GET,8844,"localhost","/test/event-source")
                .putHeader("content-type","text/event-stream")
                .send(ar -> {
                    if (ar.succeeded()) {
                        log.info("请求结果：{}", ar.result().body().toString("UTF-8"));
                    } else {
                        log.error("请求错误", ar.cause());
                    }
                });

//        HttpRequest request;
//        new StreamingBodyCodec()
        Thread.sleep(20000);
    }
}
