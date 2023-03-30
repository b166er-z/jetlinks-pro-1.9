package org.jetlinks.pro.network.coap.codec;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.jetlinks.core.message.codec.DefaultCoapMessage;
import org.jetlinks.pro.network.coap.client.DefaultCoapClient;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CoapRuleDataCodecTest {

    @Test
    void testCodec() {
        CoapRuleDataCodec codec = new CoapRuleDataCodec();

        DefaultCoapMessage message = new DefaultCoapMessage();
        message.setPayload(PayloadType.STRING.write("test"));
        message.setOptions(Arrays.asList(new Option(10001,"test")));
        message.setPath("/admin");

        Map<String, Object> data = (Map<String, Object>) codec.encode(message, PayloadType.STRING);
        Assert.assertEquals(data.get("payloadType"), "STRING");
        Assert.assertEquals(data.get("payload"), "test");


        codec.decode(RuleData.create(data))
                .as(StepVerifier::create)
                .expectNextMatches(coapMessage -> coapMessage.getOption(10001).isPresent()
                        && PayloadType.STRING.read(coapMessage.getPayload()).equals("test"))
                .verifyComplete();


    }

    @Test
    void testOptions() {
        Assert.assertTrue(CoapRuleDataCodec.toOptions((Map<String, Object>) null).isEmpty());
        Assert.assertTrue(CoapRuleDataCodec.toOptions((Collection) null).isEmpty());


        Map<String, Object> options = CoapRuleDataCodec.toOptions(Arrays.asList(
                new Option(1001, "custom"),
                new Option(OptionNumberRegistry.CONTENT_FORMAT, MediaTypeRegistry.APPLICATION_JSON)
        ));


        System.out.println(options);
        Assert.assertEquals(options.get("1001"), "custom");
        Assert.assertEquals(options.get("Content-Format"), 50);

    }

}