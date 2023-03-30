package org.jetlinks.pro.device.function;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeviceMetadataPropertyFunction extends FunctionMapFeature {
    public DeviceMetadataPropertyFunction(DeviceRegistry registry) {
        super("device.metadata.property", 2, 2, args -> args
            .collectList()
            .flatMap(arg -> {
                String deviceId = String.valueOf(arg.get(0));
                String property = String.valueOf(arg.get(1));
                return registry.getDevice(deviceId)
                               .flatMap(DeviceOperator::getMetadata)
                               .flatMap(metadata -> Mono
                                   .justOrEmpty(metadata.getPropertyOrNull(property)));
            }));
    }
}
