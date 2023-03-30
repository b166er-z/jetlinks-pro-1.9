package org.jetlinks.pro.gateway;

import org.jetlinks.pro.gateway.supports.DeviceGatewayProvider;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DeviceGatewayManager {

    Mono<DeviceGateway> getGateway(String id);

    Mono<Void> shutdown(String gatewayId);

    Mono<Void> start(String id);

    List<DeviceGatewayProvider> getProviders();
}
