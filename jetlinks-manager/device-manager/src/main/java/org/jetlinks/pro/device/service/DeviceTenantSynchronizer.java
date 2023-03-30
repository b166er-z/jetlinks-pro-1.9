package org.jetlinks.pro.device.service;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.pro.PropertyConstants;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.events.DeviceDeployedEvent;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.tenant.AssetManager;
import org.jetlinks.pro.tenant.TenantAsset;
import org.jetlinks.pro.tenant.event.AssetsBindEvent;
import org.jetlinks.pro.tenant.event.AssetsUnBindEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备和租户关联同步器,用于设备关联了租户时,将租户信息同步到设备配置中,
 * 以便在进行消息转发时,可获取设备消息的租户信息
 *
 * @author zhouhao
 * @see org.jetlinks.pro.device.message.DeviceMessageConnector
 * @since 1.6
 */
@Component
@AllArgsConstructor
public class DeviceTenantSynchronizer {

    private final DeviceRegistry deviceRegistry;

    private final AssetManager assetManager;

    /**
     * 监听租户资产绑定事件
     * @param event 事件
     */
    @EventListener
    public void handleBindEvent(AssetsBindEvent event) {
        if (DeviceAssetType.device.getId().equals(event.getAssetType())) {
            event.async(
                syncDeviceTenant(event.getAssetId())
            );
        }
    }

    @EventListener
    public void handleUnBindEvent(AssetsUnBindEvent event) {
        if (DeviceAssetType.device.getId().equals(event.getAssetType())) {
            event.async(
                syncDeviceTenant(event.getAssetId())
            );
        }
    }

    @EventListener
    public void handleDeviceDeploy(DeviceDeployedEvent event) {
        event.async(
            syncDeviceTenant(
                event.getDevices().stream().map(DeviceInstanceEntity::getId).collect(Collectors.toList())
            ));
    }

    public Mono<Void> syncDeviceTenant(Collection<String> deviceIds) {
        Set<String> noBindDevice = new HashSet<>(deviceIds);
        return assetManager
            .getTenantAssets(DeviceAssetType.device, deviceIds)
            .groupBy(TenantAsset::getAssetId, Integer.MAX_VALUE)
            .flatMap(group -> {
                String deviceId = group.key();
                noBindDevice.remove(deviceId);
                Flux<TenantAsset> cache = group.cache();
                return Mono
                    .zip(
                        //设备
                        deviceRegistry.getDevice(deviceId),
                        //租户
                        cache.map(TenantAsset::getTenantId).distinct().collectList(),
                        //成员
                        cache.map(TenantAsset::getOwnerId).distinct().collectList()
                    )
                    //设置到配置中
                    .flatMap(tp2 -> tp2.getT1()
                                       .setConfigs(PropertyConstants.tenantId.value(tp2.getT2()),
                                                   PropertyConstants.tenantMemberId.value(tp2.getT3())));
            })
            .then(Flux
                      .fromIterable(noBindDevice)
                      .flatMap(deviceRegistry::getDevice)
                      .flatMap(device -> device.removeConfigs(PropertyConstants.tenantId,
                                                              PropertyConstants.tenantMemberId))
                      .then()
            );
    }

}
