package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.pro.device.entity.DeviceFirmwareInfoEntity;
import org.jetlinks.pro.device.entity.PushFirmwareResponse;
import org.jetlinks.pro.device.service.DeviceFirmwareService;
import org.jetlinks.pro.device.service.FirmwareUpgradeHistoryService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.crud.TenantCorrelatesAccessCrudController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.function.Function;

@RestController
@RequestMapping("/device/firmware")
@Resource(id = "device-firmware-manager", name = "设备固件信息管理")
@Tag(name = "设备固件信息管理")
public class DeviceFirmwareController implements TenantCorrelatesAccessCrudController<DeviceFirmwareInfoEntity, String> {

    private final DeviceFirmwareService firmwareService;

    public DeviceFirmwareController(DeviceFirmwareService firmwareService) {
        this.firmwareService = firmwareService;
    }

    @Override
    public DeviceFirmwareService getService() {
        return firmwareService;
    }

    @Nonnull
    @Override
    public AssetType getAssetType() {
        return DeviceAssetType.device;
    }

    @Nonnull
    @Override
    public Function<DeviceFirmwareInfoEntity, ?> getAssetIdMapper() {
        return DeviceFirmwareInfoEntity::getId;
    }

    @Nonnull
    @Override
    public String getAssetProperty() {
        return "id";
    }
}
