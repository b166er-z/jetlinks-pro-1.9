package org.jetlinks.pro.rule.engine.executor;

import org.jetlinks.core.device.DeviceOperator;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface DeviceSelector {

    Flux<DeviceOperator> select(Map<String,Object> context);

}
