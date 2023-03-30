package org.jetlinks.pro.network.websocket.executor;

import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class WebSocketMessageCodec implements RuleDataCodec<WebSocketMessage> {

    private static final WebSocketMessageCodec instance = new WebSocketMessageCodec();

    static {
        RuleDataCodecs.register(WebSocketMessage.class, instance);
    }

    static void register() {
    }

    @Override
    public Object encode(WebSocketMessage data, Feature... features) {
        PayloadType payloadType = Feature.find(PayloadType.class, features)
            .orElse(PayloadType.BINARY);

        Map<String, Object> map = new HashMap<>();
        map.put("payload", payloadType.read(data.getPayload()));
        map.put("payloadType", payloadType.name());

        return map;
    }

    @Override
    public Flux<WebSocketMessage> decode(RuleData data, Feature... features) {
        return data
            .dataToMap()
            .flatMap(map -> {
                Object payload = map.get("payload");
                if (payload == null) {
                    return Mono.empty();
                }
                PayloadType payloadType = Feature
                    .find(PayloadType.class, features)
                    .orElse(PayloadType.BINARY);

                DefaultWebSocketMessage message = new DefaultWebSocketMessage();
                message.setPayload(payloadType.write(payload));
                message.setType(payloadType == PayloadType.BINARY ? WebSocketMessage.Type.BINARY : WebSocketMessage.Type.TEXT);
                return Mono.just(message);
            });
    }
}
