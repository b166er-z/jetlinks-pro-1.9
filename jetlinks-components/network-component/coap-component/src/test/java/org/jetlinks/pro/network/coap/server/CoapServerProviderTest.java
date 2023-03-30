package org.jetlinks.pro.network.coap.server;

import lombok.SneakyThrows;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.jetlinks.pro.network.coap.client.CoapClientProperties;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class CoapServerProviderTest {

    static {
        NetworkConfig.setStandard(new NetworkConfig());
    }

    @Test
    @SneakyThrows
    void test() {
        CoapServerProvider provider = new CoapServerProvider(certId -> Mono.empty());

        CoapServerProperties properties = new CoapServerProperties();
        properties.setPort(10221);

        CoapServer server = provider.createNetwork(properties);

        server.subscribe()
                .subscribe(exchange -> {
                    exchange.respond("ok");
                });

        CoapClient client = new CoapClient();

        Assert.assertEquals(client.advanced(Request.newGet().setURI("coap://127.0.0.1:10221"))
                .getResponseText(), "ok");


    }

}