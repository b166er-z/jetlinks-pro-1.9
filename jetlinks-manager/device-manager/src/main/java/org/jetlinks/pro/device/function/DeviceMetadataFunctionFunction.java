package org.jetlinks.pro.device.function;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeviceMetadataFunctionFunction extends FunctionMapFeature {
    public DeviceMetadataFunctionFunction(DeviceRegistry registry) {
        super("device.metadata.function", 2, 2, args -> args
            .collectList()
            .flatMap(arg -> {
                String deviceId = String.valueOf(arg.get(0));
                String function = String.valueOf(arg.get(1));
                return registry.getDevice(deviceId)
                               .flatMap(DeviceOperator::getMetadata)
                               .flatMap(metadata -> Mono
                                   .justOrEmpty(metadata.getFunctionOrNull(function)));
            }));
    }
}
