package org.jetlinks.pro.tenant.supports;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.jetlinks.pro.tenant.AssetManager;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.CurrentTenantSupplier;
import org.jetlinks.pro.tenant.dimension.TenantDimensionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Component
@ConditionalOnBean(AssetManager.class)
public class DefaultCurrentTenantSupplier implements CurrentTenantSupplier {

    private final AssetManager assetManager;

    public DefaultCurrentTenantSupplier(AssetManager assetManager) {
        this.assetManager = assetManager;
        CurrentTenantHolder.addSupplier(this);
    }

    @Override
    public Mono<TenantMember> get() {
        return Authentication.currentReactive()
            .flatMap(this::fromAuth);
    }

    @Override
    public Mono<TenantMember> fromAuth(Authentication auth) {
        return Flux.fromIterable(auth.getDimensions())
            .filter(dimension -> TenantDimensionType.any(dimension.getType()))
            .<TenantMember>map(dimension -> new DefaultTenantMember(
                auth.getUser().getId(),
                new DefaultTenant(dimension.getId(), dimension.getName()),
                assetManager,
                dimension.getOption("admin").map(Boolean.TRUE::equals).orElse(false),
                dimension.getOption("main").map(Boolean.TRUE::equals).orElse(false)))
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .map(members -> {
                if (members.size() == 1) {
                    return members.get(0);
                }
                return new MultiTenantMember(auth.getUser().getId(), members);
            });
    }
}
