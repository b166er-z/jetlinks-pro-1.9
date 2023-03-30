package org.jetlinks.pro.rule.engine.tenant;

import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.service.RuleInstanceService;
import org.jetlinks.pro.tenant.AssetSupplier;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.supports.DefaultAsset;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class RuleEngineAssetSupplier implements AssetSupplier {

    private final RuleInstanceService service;

    public RuleEngineAssetSupplier(RuleInstanceService configService) {
        this.service = configService;
    }

    @Override
    public List<AssetType> getTypes() {
        return Collections.singletonList(RuleEngineAssetType.ruleInstance);
    }

    @Override
    public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> assetId) {
        return service.createQuery()
            .where()
            .in(RuleInstanceEntity::getId, assetId)
            .fetch()
            .map(config -> new DefaultAsset(config.getId(), config.getName(),RuleEngineAssetType.ruleInstance));
    }
}
