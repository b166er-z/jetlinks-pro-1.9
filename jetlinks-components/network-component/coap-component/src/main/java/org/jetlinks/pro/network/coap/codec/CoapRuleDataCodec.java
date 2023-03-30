package org.jetlinks.pro.network.coap.codec;

import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.core.message.codec.DefaultCoapMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CoAP规则数据编解码器,用于在规则引擎中使用CoAP时，将CoAP数据与规则数据进行互相转换
 *
 * @author
 * @since 1.0
 */
public class CoapRuleDataCodec implements RuleDataCodec<CoapMessage> {
    private static final CoapRuleDataCodec instance = new CoapRuleDataCodec();

    static {
        RuleDataCodecs.register(CoapMessage.class, instance);
    }

    public static void register() {
    }

    @Override
    public Object encode(CoapMessage data, Feature... features) {

        PayloadType payloadType = Feature.find(PayloadType.class, features).orElse(PayloadType.JSON);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("payloadType", payloadType.name());
        map.put("options", toOptions(data.getOptions()));
        map.put("payload", payloadType.read(data.getPayload()));
        map.put("path", data.getPath());
        return map;
    }

    @Override
    public Flux<CoapMessage> decode(RuleData data, Feature... features) {
        return data.dataToMap()
                   .flatMap(map -> Mono
                       .justOrEmpty(map.get("payloadType"))
                       .map(String::valueOf)
                       .map(PayloadType::valueOf)
                       .switchIfEmpty(Mono.justOrEmpty(Feature.find(PayloadType.class, features)))
                       .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("无法转换为CoAP消息")))
                       .flatMap(payloadType -> Mono
                           .justOrEmpty(map.get("payload"))
                           .map(payloadType::write)
                           .map(payload -> {
                               DefaultCoapMessage message = new DefaultCoapMessage();
                               Optional.ofNullable(map.get("path"))
                                       .map(String::valueOf)
                                       .ifPresent(message::setPath);
                               message.setPayload(payload);
                               message.setOptions(toOptions((Map) map.get("options")));
                               return message;
                           })
                           .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("CoAP消息为空")))));
    }


    public static List<Option> toOptions(Map<String, Object> options) {
        if (!CollectionUtils.isEmpty(options)) {
            return options
                .entrySet()
                .stream()
                .map(kv -> {
                    int num = OptionNumberRegistry.toNumber(kv.getKey());
                    if (num == -1) {
                        num = Integer.parseInt(kv.getKey());
                    }
                    Object value = kv.getValue();

                    if (OptionNumberRegistry.getFormatByNr(num) == OptionNumberRegistry.optionFormats.INTEGER) {
                        if (value instanceof String) {
                            value = new BigDecimal(String.valueOf(value));
                        }
                        if (value instanceof Number) {
                            return new Option(num, ((Number) value).longValue());
                        }
                    }
                    // TODO: 2019-11-15 其他类型

                    return new Option(num, String.valueOf(value));
                }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static Map<String, Object> toOptions(Collection<Option> options) {
        if (CollectionUtils.isEmpty(options)) {
            return Collections.emptyMap();
        }
        Map<String, Object> optionsMap = new LinkedHashMap<>();
        for (Option option : options) {
            Object val;
            OptionNumberRegistry.optionFormats formats = OptionNumberRegistry.getFormatByNr(option.getNumber());
            if (formats == OptionNumberRegistry.optionFormats.INTEGER) {
                val = option.getIntegerValue();
            } else {
                val = option.getStringValue();
            }
            if (formats != OptionNumberRegistry.optionFormats.UNKNOWN) {
                optionsMap.put(OptionNumberRegistry.toString(option.getNumber()), val);
            } else {
                optionsMap.put(String.valueOf(option.getNumber()), val);
            }
        }
        return optionsMap;
    }
}
