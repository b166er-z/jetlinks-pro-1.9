package org.jetlinks.pro.network.udp.codec;

import org.jetlinks.pro.network.udp.UdpMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * UDP消息与规则数据编解码器,在规则引擎中使用UDP时，使用此编解码器进行数据转换。
 * 输入，输出格式为:
 * <pre>
 *     {"payload":"udp报文"}
 * </pre>
 *
 * @author zhouhao
 * @since 1.0
 */
public class UdpMessageCodec implements RuleDataCodec<UdpMessage> {

    private static final UdpMessageCodec instance = new UdpMessageCodec();

    static {
        RuleDataCodecs.register(UdpMessage.class, instance);
    }

    public static void register() {
    }

    @Override
    public Object encode(UdpMessage data, Feature... features) {
        PayloadType payloadType = Feature.find(PayloadType.class, features)
                                         .orElse(PayloadType.BINARY);

        Map<String, Object> map = new HashMap<>();
        map.put("payload", payloadType.read(data.getPayload()));
        map.put("payloadType", payloadType.name());

        return map;
    }

    @Override
    public Flux<UdpMessage> decode(RuleData data, Feature... features) {
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

                UdpMessage message = new UdpMessage();
                message.setPayload(payloadType.write(payload));
                // message.setPayloadType(MessagePayloadType.valueOf(payloadType.name()));

                return Mono.just(message);
            });
    }
}
