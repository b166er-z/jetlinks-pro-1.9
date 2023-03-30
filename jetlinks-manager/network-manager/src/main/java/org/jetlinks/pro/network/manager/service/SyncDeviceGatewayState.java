package org.jetlinks.pro.network.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.DeviceGatewayManager;
import org.jetlinks.pro.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.pro.network.manager.enums.NetworkConfigState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author wangzheng
 * @since 1.0
 */
@Order(1)
@Component
@Slf4j
public class SyncDeviceGatewayState implements CommandLineRunner {

    private final DeviceGatewayService deviceGatewayService;

    private final DeviceGatewayManager deviceGatewayManager;

    private final Duration gatewayStartupDelay = Duration.ofSeconds(5);

    public SyncDeviceGatewayState(DeviceGatewayService deviceGatewayService, DeviceGatewayManager deviceGatewayManager) {
        this.deviceGatewayService = deviceGatewayService;
        this.deviceGatewayManager = deviceGatewayManager;
    }

    @Override
    public void run(String... args) {
        log.debug("start device gateway in {} later", gatewayStartupDelay);
        Mono.delay(gatewayStartupDelay)
            .then(
                deviceGatewayService
                    .createQuery()
                    .where()
                    .and(DeviceGatewayEntity::getState, NetworkConfigState.enabled)
                    .fetch()
                    .map(DeviceGatewayEntity::getId)
                    .flatMap(id -> deviceGatewayManager
                        .getGateway(id)
                        .flatMap(DeviceGateway::startup)
                        .onErrorResume((err) -> {
                            log.error(err.getMessage(), err);
                            return Mono.empty();
                        }))
                    .then()
            )
            .subscribe();
    }
}
