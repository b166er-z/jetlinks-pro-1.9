package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.pro.device.entity.FirmwareUpgradeHistoryEntity;
import org.jetlinks.pro.device.entity.FirmwareUpgradeTaskEntity;
import org.jetlinks.pro.device.service.FirmwareUpgradeHistoryService;
import org.jetlinks.pro.device.service.FirmwareUpgradeTaskService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.crud.TenantCorrelatesAccessCrudController;
import org.jetlinks.pro.tenant.crud.TenantCorrelatesAccessQueryController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.function.Function;

@RestController
@RequestMapping("/firmware/upgrade/history")
@Resource(id = "firmware-upgrade-task-manager", name = "固件升级任务管理")
@Tag(name = "固件升级记录")
public class FirmwareUpgradeHistoryController implements TenantCorrelatesAccessQueryController<FirmwareUpgradeHistoryEntity, String> {

    private final FirmwareUpgradeHistoryService historyService;

    public FirmwareUpgradeHistoryController(FirmwareUpgradeHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public FirmwareUpgradeHistoryService getService() {
        return historyService;
    }

    @Nonnull
    @Override
    public AssetType getAssetType() {
        return DeviceAssetType.device;
    }

    @Nonnull
    @Override
    public Function<FirmwareUpgradeHistoryEntity, ?> getAssetIdMapper() {
        return FirmwareUpgradeHistoryEntity::getDeviceId;
    }

    @Nonnull
    @Override
    public String getAssetProperty() {
        return "deviceId";
    }
}
