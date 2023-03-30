package org.jetlinks.pro.network.manager.debug;

import org.jetlinks.core.message.codec.*;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.coap.client.CoapClient;
import org.jetlinks.pro.network.coap.server.CoapServer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CoAPDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public CoAPDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-coap-debug";
    }

    @Override
    public String name() {
        return "CoAP调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/coap/client/*/_send",
            "/network/coap/server/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        if (request.getTopic().startsWith("/network/coap/server")) {
            return subscribeServer(id, request);
        } else {
            return subscribeClient(id, request);
        }
    }

    static CoapResponseMessage DEFAULT_RESPONSE =
        DefaultCoapResponseMessage.of(
            String.join("\n"
                , "CREATED 2.01"
                , "Content-Format application/json"
                , ""
                , "{\"success\":true}"
            )
        );

    public Flux<String> subscribeClient(String id, SubscribeRequest request) {
        CoapMessage message = request.getString("request")
            .map(DefaultCoapMessage::of)
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));
        return networkManager
            .<CoapClient>getNetwork(DefaultNetworkType.COAP_CLIENT, id)
            .flatMapMany(client -> client
                .publish(message)
                .map(msg -> msg.print(false)));
    }

    public Flux<String> subscribeServer(String id, SubscribeRequest request) {
        CoapResponseMessage response = request.getString("response")
            .map(CoapResponseMessage::fromText)
            .orElse(DEFAULT_RESPONSE);

        return networkManager
            .<CoapServer>getNetwork(DefaultNetworkType.COAP_SERVER, id)
            .flatMapMany(server -> server
                .subscribe()
                .map(exchange -> {
                    CoapExchangeMessage msg=new CoapExchangeMessage(exchange);

                    msg.response(response);
                    StringBuilder builder = new StringBuilder("/*    from ")
                        .append(exchange.getSourceSocketAddress().toString())
                        .append("    */");
                    builder.append("\n\n");
                    builder.append(msg.print(false));
                    builder.append("\n/*    response    */\n\n");
                    builder.append(response.print(false));
                    return builder.toString();

                }));
    }


}
