package org.jetlinks.pro.network.manager.web.request;

import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.jetlinks.pro.network.coap.client.CoapClientProperties;
import org.jetlinks.pro.network.coap.codec.CoapRuleDataCodec;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class CoapMessageRequest {

    private String url;

    private String options;

    private CoAP.Code method = CoAP.Code.POST;

    private Object payload;

    private PayloadType payloadType;

    public static CoapMessageRequest of(CoapExchange exchange, PayloadType payloadType) {
        CoapMessageRequest request = new CoapMessageRequest();
        request.setUrl(exchange.getRequestOptions().getLocationPathString());
        request.options = CoapRuleDataCodec.toOptions(exchange.getRequestOptions().asSortedList())
                .entrySet()
                .stream()
                .map(e -> e.getKey().concat(": ").concat(String.valueOf(e.getValue())))
                .collect(Collectors.joining("\n"));
        request.method = exchange.getRequestCode();
        request.payload = exchange.getRequestPayload() == null ? "" : payloadType.read(Unpooled.wrappedBuffer(exchange.getRequestPayload()));

        return request;
    }

    public Request toRequest(CoapClientProperties properties) {
        Request request = new Request(method);
        if (StringUtils.hasText(options)) {
            CoapRuleDataCodec.toOptions(Stream.of(options.split("\n"))
                    .map(str -> str.split("[:：，,=]", 2))
                    .filter(arr -> arr.length > 1)
                    .collect(Collectors.toMap(arr -> arr[0].trim(), arr -> arr[1].trim(), (_1, _2) -> _1)))
                    .forEach(request.getOptions()::addOption);
        }
        if (getPayload() != null) {
            request.setPayload(getPayloadType().write(getPayload()).array());
        }
        if (StringUtils.hasText(properties.getUrl()) && !url.startsWith("coap://")) {
            request.setURI(properties.getUrl().concat(url));
        } else {
            request.setURI(url);
        }
        return request;
    }


}
