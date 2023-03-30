package org.jetlinks.pro.network.coap.executor;

import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.pro.network.coap.codec.CoapRuleDataCodec;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class CoapServerTaskConfiguration {

    static {
        CoapRuleDataCodec.register();
    }

    private String serverId;

    private String options;

    private String code;

    private PayloadType payloadType = PayloadType.STRING;

    public void validate() {
        Assert.hasText(code, "code can not be empty");
    }

    public Flux<Object> doRespond(CoapExchange exchange, RuleData ruleData) {
        return Flux.defer(() -> RuleDataCodecs.<CoapMessage>getCodec(CoapMessage.class)
                .map(codec -> codec.decode(ruleData, payloadType))
                .orElseThrow(() -> new UnsupportedOperationException("无法转码为CoAP消息:" + ruleData.getData()))
                .map(msg -> {
                    Response response = new Response(CoAP.ResponseCode.valueOf(code));
                    response.setPayload(payloadType.write(msg.getPayload()).array());
                    exchange.respond(response);
                    Map<String, Object> map = new HashMap<>();
                    map.put("url", exchange.getRequestOptions().getUriPathString());
                    map.put("options", exchange.getRequestOptions().asSortedList());
                    map.put("method", exchange.getRequestCode());
                    map.put("payload", exchange.getRequestPayload() == null ? "" : payloadType.read(Unpooled.wrappedBuffer(exchange.getRequestPayload())));
                    return map;
                }));
    }

}
