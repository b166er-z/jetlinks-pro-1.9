package org.jetlinks.pro.device.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.*;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.enums.DeviceState;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * 自动发现设备的注册中心,当默认注册中心没有获取到设备信息时，尝试查询数据库来获取设备信息。
 *
 * @author zhouhao
 */
public class AutoDiscoverDeviceRegistry implements DeviceRegistry {

    private final DeviceRegistry parent;

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;

    private final ReactiveRepository<DeviceProductEntity, String> productRepository;

    public AutoDiscoverDeviceRegistry(DeviceRegistry parent,
                                      ReactiveRepository<DeviceInstanceEntity, String> deviceRepository,
                                      ReactiveRepository<DeviceProductEntity, String> productRepository) {
        this.parent = parent;
        this.deviceRepository = deviceRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Mono<DeviceOperator> getDevice(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return Mono.empty();
        }
        return Mono.defer(() -> parent
            .getDevice(deviceId)
            .switchIfEmpty(Mono.defer(() -> deviceRepository
                .findById(deviceId)
                .filter(instance -> instance.getState() != DeviceState.notActive)
                .flatMap(instance -> parent.register(instance.toDeviceInfo())))
            )
        );
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId) {
        if (StringUtils.isEmpty(productId)) {
            return Mono.empty();
        }
        return parent
            .getProduct(productId)
            .switchIfEmpty(Mono.defer(() -> productRepository
                .findById(productId)
                .filter(product -> product.getState() == 1)
                .flatMap(product -> parent.register(product.toProductInfo()))));
    }

    @Override
    public Mono<DeviceOperator> register(DeviceInfo deviceInfo) {
        return parent.register(deviceInfo);
    }

    @Override
    public Mono<DeviceProductOperator> register(ProductInfo productInfo) {
        return parent.register(productInfo);
    }

    @Override
    public Mono<Void> unregisterDevice(String deviceId) {
        return parent
            .getDevice(deviceId)
            .flatMap(DeviceOperator::disconnect)//注销时，尝试断开连接。
            .onErrorResume(err -> Mono.empty())
            .then(parent.unregisterDevice(deviceId));
    }

    @Override
    public Mono<Void> unregisterProduct(String productId) {
        return parent.unregisterProduct(productId);
    }
}
