package org.jetlinks.pro.notify.manager.tenant;

import org.jetlinks.pro.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.pro.notify.manager.service.NotifyTemplateService;
import org.jetlinks.pro.tenant.AssetSupplier;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.supports.DefaultAsset;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class NotifyTemplateAssetSupplier implements AssetSupplier {

    private final NotifyTemplateService service;

    public NotifyTemplateAssetSupplier(NotifyTemplateService configService) {
        this.service = configService;
    }

    @Override
    public List<AssetType> getTypes() {
        return Collections.singletonList(NotifyAssetType.notifyTemplate);
    }

    @Override
    public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> assetId) {
        return service.createQuery()
            .where()
            .in(NotifyTemplateEntity::getId, assetId)
            .fetch()
            .map(config -> new DefaultAsset(config.getId(), config.getName(),NotifyAssetType.notifyTemplate));
    }
}
