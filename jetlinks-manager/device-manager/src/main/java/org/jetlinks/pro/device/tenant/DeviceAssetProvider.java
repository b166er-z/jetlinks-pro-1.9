package org.jetlinks.pro.device.tenant;

import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.tenant.AssetSupplier;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.supports.DefaultAsset;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
public class DeviceAssetProvider implements AssetSupplier {

    private final LocalDeviceInstanceService instanceService;

    private final LocalDeviceProductService productService;

    public DeviceAssetProvider(LocalDeviceInstanceService instanceService,
                               LocalDeviceProductService productService) {
        this.instanceService = instanceService;
        this.productService = productService;
    }

    @Override
    public List<AssetType> getTypes() {
        return Arrays.asList(DeviceAssetType.values());
    }

    @Override
    public Flux<DefaultAsset> getAssets(AssetType type,
                                        Collection<?> assetId) {

        return type == DeviceAssetType.device
            ? getDevice(assetId)
            : getProduct(assetId);
    }

    @SuppressWarnings("all")
    private Flux<DefaultAsset> getProduct(Collection<?> assertId) {

        return productService
            .findById((Collection<String>)assertId)
            .map(instance -> new DefaultAsset(instance.getId(), instance.getName(), DeviceAssetType.product));
    }

    @SuppressWarnings("all")
    private Flux<DefaultAsset> getDevice(Collection<?> assertId) {

        return instanceService
            .findById((Collection<String>)assertId)
            .map(instance -> new DefaultAsset(instance.getId(), instance.getName(), DeviceAssetType.device));
    }
}
