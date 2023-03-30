package org.jetlinks.pro.network.coap.client;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CoapClientProviderTest {

    static {
        NetworkConfig.setStandard(new NetworkConfig());
    }

    @Test
    void test() {
        CoapClientProvider provider = new CoapClientProvider(id -> Mono.empty());
        CoapClientProperties properties = new CoapClientProperties();
        properties.setTimeout(10);
        CoapClient coapClient = provider.createNetwork(properties);
        Assert.assertNotNull(coapClient);
        provider.reload(coapClient,properties);
    }

    @Test
    void testMessage(){
        CoapServer server = new CoapServer();

        server.addEndpoint(new CoapEndpoint.Builder()
                .setPort(12345)
                .build());
        server.add(new CoapResource("test") {
            @Override
            public void handlePOST(CoapExchange exchange) {

                exchange.respond("ok");
            }
        });
        server.start();

        CoapClientProvider provider = new CoapClientProvider(id -> Mono.empty());
        CoapClientProperties properties = new CoapClientProperties();
        properties.setTimeout(10);
        CoapClient coapClient = provider.createNetwork(properties);

        coapClient.publish(Request.newPost()
                .setURI("coap://localhost:12345/test")
                .setPayload("test"))
                .as(StepVerifier::create)
                .expectNextMatches(resp -> resp.isSuccess() && resp.getResponseText().equals("ok"))
                .verifyComplete();
    }

}