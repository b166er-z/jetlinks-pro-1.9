package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.device.entity.FirmwareEntity;
import org.jetlinks.pro.device.service.FirmwareService;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.crud.TenantCorrelatesAccessCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import java.util.function.Function;

@RestController
@RequestMapping("/firmware")
@Resource(id = "firmware-manager", name = "固件管理")
@Tag(name = "固件管理")
public class FirmwareController implements TenantCorrelatesAccessCrudController<FirmwareEntity, String> {

    private final FirmwareService firmwareService;

    public FirmwareController(FirmwareService firmwareService) {
        this.firmwareService = firmwareService;
    }

    @Override
    public FirmwareService getService() {
        return firmwareService;
    }

    @Nonnull
    @Override
    public AssetType getAssetType() {
        return DeviceAssetType.product;
    }

    @Nonnull
    @Override
    public Function<FirmwareEntity, ?> getAssetIdMapper() {
        return FirmwareEntity::getProductId;
    }

    @Nonnull
    @Override
    public String getAssetProperty() {
        return "productId";
    }
}
