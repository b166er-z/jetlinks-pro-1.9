package org.jetlinks.pro.network.manager.tenant;

import org.jetlinks.pro.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.pro.network.manager.service.CertificateService;
import org.jetlinks.pro.network.manager.service.NetworkConfigService;
import org.jetlinks.pro.tenant.AssetSupplier;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.supports.DefaultAsset;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class CertificateAssetSupplier implements AssetSupplier {

    private final CertificateService service;

    public CertificateAssetSupplier(CertificateService configService) {
        this.service = configService;
    }

    @Override
    public List<AssetType> getTypes() {
        return Collections.singletonList(NetworkAssetType.certificate);
    }

    @Override
    public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> assetId) {
        return service.createQuery()
            .where()
            .in(NetworkConfigEntity::getId, assetId)
            .fetch()
            .map(config -> new DefaultAsset(config.getId(), config.getName(), NetworkAssetType.network));
    }
}
