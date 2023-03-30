package org.jetlinks.pro.network.coap.executor;

import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.Request;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.pro.network.coap.client.CoapClient;
import org.jetlinks.pro.network.coap.codec.CoapRuleDataCodec;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class CoapClientTaskConfiguration {

    static {
        CoapRuleDataCodec.register();
    }

    private String uri;

    private String clientId;

    private PayloadType payloadType = PayloadType.JSON;

    private CoAP.Code method = CoAP.Code.POST;

    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty");
        Assert.hasText(uri, "uri can not be empty");

    }

    public Flux<Object> doSend(CoapClient coapClient, RuleData ruleData) {
        return Flux.defer(() ->
                RuleDataCodecs.<CoapMessage>getCodec(CoapMessage.class)
                        .map(codec -> codec.decode(ruleData, payloadType))
                        .orElseThrow(() -> new UnsupportedOperationException("无法转码为CoAP消息:" + ruleData.getData()))
                        .map(msg -> this.createRequest(coapClient, msg))
                        .flatMap(coapClient::publish)
                        .map(response -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("code", response.getCode().name());
                            map.put("success", response.isSuccess());
                            map.put("options", CoapRuleDataCodec.toOptions(response.getOptions().asSortedList()));
                            map.put("path", response.getOptions().getUriPathString());
                            byte[] payload = response.getPayload();
                            if (payload != null) {
                                map.put("payload", payloadType.read(Unpooled.wrappedBuffer(payload)));
                            }
                            return map;
                        }));

    }

    protected Request createRequest(CoapClient client, CoapMessage coapMessage) {
        Request request = new Request(method);
        String url = StringUtils.hasText(coapMessage.getPath()) ? coapMessage.getPath() : this.uri;
        if (null != url) {
            if (!url.startsWith("coap://")) {
                url = client
                        .getProperties()
                        .getUrl()
                        .concat(this.uri);
            }
        } else {
            url = client.getProperties().getUrl();
        }
        request.setURI(url);
        for (Option option : coapMessage.getOptions()) {
            request.getOptions().addOption(option);
        }
        request.setPayload((byte[]) PayloadType.BINARY.read(coapMessage.getPayload()));
        return request;
    }

}
