package org.jetlinks.pro.network.manager.debug;

import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.core.message.codec.http.SimpleHttpRequestMessage;
import org.jetlinks.core.message.codec.http.SimpleHttpResponseMessage;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.http.client.HttpClient;
import org.jetlinks.pro.network.http.server.HttpRequest;
import org.jetlinks.pro.network.http.server.HttpServer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class HttpDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public HttpDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-http-debug";
    }

    @Override
    public String name() {
        return "HTTP调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/http/client/*/_send",
            "/network/http/server/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        if (request.getTopic().startsWith("/network/http/server")) {
            return subscribeServer(id, request);
        } else {
            return subscribeClient(id, request);
        }
    }

    static HttpResponseMessage DEFAULT_RESPONSE =
        SimpleHttpResponseMessage.of(
            String.join("\n"
                , "HTTP/1.1 200 OK"
                , "Content-Type application/json"
                , ""
                , "{\"success\":true}"
            )
        );

    public Flux<String> subscribeClient(String id, SubscribeRequest request) {
        HttpRequestMessage message = request.getString("request")
            .map(SimpleHttpRequestMessage::of)
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));
        return networkManager
            .<HttpClient>getNetwork(DefaultNetworkType.HTTP_CLIENT, id)
            .flatMapMany(client -> client
                .request(message)
                .map(HttpResponseMessage::print));
    }

    public Flux<String> subscribeServer(String id, SubscribeRequest request) {
        HttpResponseMessage response = request.getString("response")
            .<HttpResponseMessage>map(SimpleHttpResponseMessage::of)
            .orElse(DEFAULT_RESPONSE);

        return networkManager
            .<HttpServer>getNetwork(DefaultNetworkType.HTTP_SERVER, id)
            .flatMapMany(server -> server
                .handleRequest()
                .flatMap(exchange -> {
                    HttpRequest httpRequest = exchange.request();
                    return httpRequest.toMessage()
                        .flatMap(msg -> {
                            StringBuilder builder = new StringBuilder("/*    from ")
                                .append(httpRequest.getClientAddress())
                                .append("    */");
                            builder.append("\n\n");
                            builder.append(msg.print());
                            builder.append("\n/*    response    */\n\n");
                            builder.append(response.print());
                            return exchange
                                .response(response)
                                .thenReturn(builder.toString());
                        });

                }));
    }


}
