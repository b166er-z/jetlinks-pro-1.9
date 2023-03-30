package org.jetlinks.pro.network.manager.web.response;

import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.pro.network.coap.codec.CoapRuleDataCodec;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class CoapMessageResponse {

    private String options;

    private Object payload;

    private String code;

    private PayloadType payloadType = PayloadType.STRING;

    public Response toResponse() {
        Response response = new Response(CoAP.ResponseCode.valueOf(code));
        if (StringUtils.hasText(options)) {
            CoapRuleDataCodec.toOptions(Stream.of(options.split("\n"))
                    .map(str -> str.split("[:：，,=]", 2))
                    .filter(arr -> arr.length > 1)
                    .collect(Collectors.toMap(arr -> arr[0].trim(), arr -> arr[1].trim(), (_1, _2) -> _1)))
                    .forEach(response.getOptions()::addOption);
        }
        if (payload != null && payloadType != null) {
            response.setPayload(payloadType.write(payload).array());
        } else {
            response.setPayload("ok");
        }
        return response;
    }

    public static CoapMessageResponse from(CoapResponse coapResponse, PayloadType payloadType) {
        CoapMessageResponse response = new CoapMessageResponse();

        response.options = CoapRuleDataCodec.toOptions(coapResponse.getOptions().asSortedList())
                .entrySet()
                .stream()
                .map(e -> e.getKey().concat(": ").concat(String.valueOf(e.getValue())))
                .collect(Collectors.joining("\n"));

        response.payload = coapResponse.getPayload() == null ? "" : payloadType.read(Unpooled.wrappedBuffer(coapResponse.getPayload()));
        response.payloadType = payloadType;
        response.code = coapResponse.getCode().name() + "(" + coapResponse.getCode().value + ")";
        return response;
    }

}
