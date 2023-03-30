package org.jetlinks.pro.network.manager.debug;

import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.udp.UdpMessage;
import org.jetlinks.pro.network.udp.UdpSupport;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class UdpDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public UdpDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-udp-debug";
    }

    @Override
    public String name() {
        return "UDP调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/udp/*/_send",
            "/network/udp/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[3];

        if (request.getTopic().endsWith("_send")) {
            return send(id, request);
        } else {
            return subscribe(id);
        }
    }

    public Flux<String> send(String id, SubscribeRequest request) {
        UdpMessage message = request.getString("request")
            .map(UdpMessage::of)
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));

        return networkManager
            .<UdpSupport>getNetwork(DefaultNetworkType.UDP, id)
            .flatMap(client -> client.publish(message))
            .thenReturn("推送成功")
            .flux();
    }

    public Flux<String> subscribe(String id) {

        return networkManager
            .<UdpSupport>getNetwork(DefaultNetworkType.UDP, id)
            .flatMapMany(server -> server
                .subscribe()
                .map(UdpMessage::toString));
    }


}
