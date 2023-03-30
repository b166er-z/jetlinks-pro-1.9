package org.jetlinks.pro.rule.engine.function;

import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Consumer3;
import reactor.function.Function3;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean(DeviceRegistry.class)
public class DeviceMessageSendFunction extends FunctionMapFeature {

    public DeviceMessageSendFunction(DeviceRegistry registry) {
        super("device.message.send", 20, 2, args -> {
            return args.collectList()
                .flatMap(list -> {
                    String deviceId = String.valueOf(list.get(0));
                    MessageType messageType = MessageType.valueOf(String.valueOf(list.get(1)).toUpperCase());
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("deviceId", deviceId);
                    msgMap.put("messageId", IDGenerator.SNOW_FLAKE.generate());

                    if (messageType == MessageType.READ_PROPERTY) {
                        msgMap.put("properties", list.stream().skip(2).collect(Collectors.toList()));
                    } else if (messageType == MessageType.WRITE_PROPERTY) {
                        msgMap.put("properties", convertArgsMap(list.stream().skip(2).toArray(), LinkedHashMap::new, Map::put));
                    } else if (messageType == MessageType.INVOKE_FUNCTION) {
                        String functionId = String.valueOf(list.get(2));
                        msgMap.put("functionId", functionId);
                        msgMap.put("inputs", convertArgsMap(
                            list.stream().skip(3).toArray(),
                            ArrayList::new,
                            (parameters, key, value) -> parameters.add(new FunctionParameter(String.valueOf(key), value))));
                    } else {
                        return Mono.<DeviceMessageReply>empty();
                    }
                    return registry
                        .getDevice(deviceId)
                        .flatMap(device -> device
                            .messageSender()
                            .send(Mono.just(messageType.convert(msgMap)))
                            .take(1)
                            .singleOrEmpty());
                });
        });
    }


    private static <T> T convertArgsMap(Object[] arr, Supplier<T> supplier, Consumer3<T, Object, Object> consumer3) {
        T v = supplier.get();
        for (int i = 0; i < arr.length / 2; i++) {
            consumer3.accept(v, arr[i * 2], arr[i * 2 + 1]);
        }
        return v;
    }
}
