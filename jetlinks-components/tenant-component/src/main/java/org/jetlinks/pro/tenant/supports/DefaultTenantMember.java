package org.jetlinks.pro.tenant.supports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.tenant.*;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class DefaultTenantMember implements TenantMember {

    @Getter
    private final String userId;

    @Getter
    private final Tenant tenant;

    private final AssetManager assetManager;

    @Getter
    private final boolean admin;

    @Getter
    private final boolean main;

    @Override
    public @NonNull Mono<TenantAsset> getAsset(@NonNull String assetType, @NonNull String assetId) {
        return assetManager.getAssetType(assetType)
            .flatMap(type -> getAsset(type, assetId));
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull String assetType, @NonNull Collection<?> assetId) {
        return assetManager.getAssetType(assetType)
            .flatMapMany(type -> getAssets(type, assetId));
    }

    @Override
    public @NonNull Mono<TenantAsset> getAsset(@NonNull AssetType assetType,
                                               @NonNull String assetId) {
        return assetManager.getTenantAsset(tenant.getId(), assetType, isAdmin() ? null : userId, assetId);
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType,
                                                @NonNull Collection<?> assetId) {

        return assetManager.getTenantAssets(tenant.getId(), assetType, isAdmin() ? null : userId, assetId);
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType) {
        return assetManager
            .getTenantAssets(tenant.getId(), assetType, isAdmin() ? null : userId);
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull String assetType) {
        return assetManager
            .getAssetType(assetType)
            .flatMapMany(this::getAssets);
    }

    @Override
    public @NonNull Mono<Boolean> hasPermission(@NonNull String assetType,
                                                @NonNull Collection<?> assetId,
                                                boolean allowAssetNotExist,
                                                @NonNull AssetPermission... permission) {
        if (CollectionUtils.isEmpty(assetId)) {
            return Mono.just(true);
        }
        return assetManager
            .getAssetType(assetType)
            .flatMap(type -> hasPermission(type, assetId, allowAssetNotExist, permission))
            .defaultIfEmpty(false);
    }

    @Override
    public @NonNull <T> Flux<T> filter(@NonNull Flux<T> source,
                                       @NonNull AssetType assetType,
                                       @NonNull Function<T, ?> assetId,
                                       @NonNull AssetPermission... permission) {

        return source
            .buffer(200)
            .flatMap(list -> {
                Map<?, T> cache = list.stream().collect(Collectors.toMap(assetId, Function.identity()));
                return this.getAssets(assetType, cache.keySet())
                    .filter(asset -> asset.hasPermission(permission))
                    .map(asset -> cache.get(asset.getAssetId()));
            });

    }

    @Override
    public @NonNull Mono<Boolean> hasPermission(@NonNull AssetType assetType,
                                                @NonNull Collection<?> assetId,
                                                boolean allowAssetNotExist,
                                                @NonNull AssetPermission... permission) {
        if (CollectionUtils.isEmpty(assetId)) {
            return Mono.just(true);
        }
        return this
            .getAssets(assetType, assetId)
            .map(asset -> {
                if (isAdmin()) {
                    return true;
                }
                return asset.hasPermission(permission);
            })
            .switchIfEmpty(Mono.defer(() -> {
                if (!allowAssetNotExist) {
                    return Mono.just(false);
                }
                return this.assetManager
                    .getAssets(assetType, assetId)
                    .hasElements()
                    .map(exist -> !exist);
            }))
            .all(v -> v);
    }

    @Nonnull
    @Override
    public Mono<Void> bindAssets(@NonNull String assetType,
                                 @NonNull Collection<?> assetId) {

        return assetManager
            .getAssetType(assetType)
            .flatMap(type -> bindAssets(type, assetId));
    }

    @Nonnull
    @Override
    public Mono<Void> bindAssets(@NonNull AssetType assetType,
                                 @NonNull Collection<?> assetId) {
        return assetManager.bindAssetsOwner(tenant.getId(), assetType, getUserId(), assetId);
    }


    @Nonnull
    @Override
    public Mono<Void> unbindAssets(@NonNull String assetType,
                                   @NonNull Collection<?> assetId) {

        return assetManager
            .getAssetType(assetType)
            .flatMap(type -> unbindAssets(type, assetId));
    }

    @Nonnull
    @Override
    public Mono<Void> unbindAssets(@NonNull AssetType assetType,
                                   @NonNull Collection<?> assetId) {
        return assetManager.unbindAssetsOwner(tenant.getId(), assetType, getUserId(), assetId);
    }
}
