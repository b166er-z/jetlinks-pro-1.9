package org.jetlinks.pro.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * 自动同步产品名称到设备表
 *
 * @author zhouhao
 * @since 1.6
 */
@Component
@AllArgsConstructor
public class DeviceProductNameSynchronizer {

    private final LocalDeviceInstanceService instanceService;

    //自动更新产品名称
    @EventListener
    public void autoUpdateProductName(EntityModifyEvent<DeviceProductEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .filter(product-> StringUtils.hasText(product.getName()))
                .flatMap(product -> instanceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getProductName, product.getName())
                    .where(DeviceInstanceEntity::getProductId, product.getId())
                    .execute())
        );
    }

    //自动更新产品名称
    @EventListener
    public void autoUpdateProductName(EntitySavedEvent<DeviceProductEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .filter(product-> StringUtils.hasText(product.getName()))
                .flatMap(product -> instanceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getProductName, product.getName())
                    .where(DeviceInstanceEntity::getProductId, product.getId())
                    .execute())
        );
    }
}
