package org.jetlinks.pro.tenant.supports;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetlinks.pro.tenant.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MultiTenantMember implements TenantMember {

    private final String userId;

    private final List<TenantMember> members;

    public TenantMember getMain() {
        for (TenantMember member : members) {
            if (member.isMain()) {
                return member;
            }
        }
        return members.get(0);
    }

    public List<TenantMember> getMembers() {
        return new ArrayList<>(members);
    }

    public List<Tenant> getTenants() {
        return members.stream()
            .map(TenantMember::getTenant)
            .collect(Collectors.toList());
    }

    @Override
    public @NonNull Tenant getTenant() {
        return getMain().getTenant();
    }

    @Override
    public @NonNull String getUserId() {
        return userId;
    }

    @Override
    public boolean isMain() {
        return false;
    }

    @Override
    public boolean isAdmin() {
        return getMain().isAdmin();
    }

    @Override
    public @NonNull Mono<TenantAsset> getAsset(@NonNull AssetType assetType, @NonNull String assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.getAsset(assetType, assetId))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public @NonNull Mono<TenantAsset> getAsset(@NonNull String assetType, @NonNull String assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.getAsset(assetType, assetId))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType, @NonNull Collection<?> assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.getAssets(assetType, assetId));
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull String assetType) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.getAssets(assetType));
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.getAssets(assetType));
    }

    @Override
    public @NonNull Flux<TenantAsset> getAssets(@NonNull String assetType, @NonNull Collection<?> assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.getAssets(assetType, assetId));
    }

    @Nonnull
    @Override
    public Mono<Void> bindAssets(@NonNull AssetType assetType, @NonNull Collection<?> assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.bindAssets(assetType, assetId))
            .then();
    }

    @Nonnull
    @Override
    public Mono<Void> bindAssets(@NonNull String assetType, @NonNull Collection<?> assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.bindAssets(assetType, assetId))
            .then();
    }

    @Nonnull
    @Override
    public Mono<Void> unbindAssets(@NonNull AssetType assetType, @NonNull Collection<?> assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.unbindAssets(assetType, assetId))
            .then();
    }

    @Nonnull
    @Override
    public Mono<Void> unbindAssets(@NonNull String assetType, @NonNull Collection<?> assetId) {
        return Flux
            .fromIterable(members)
            .flatMap(member -> member.unbindAssets(assetType, assetId))
            .then();
    }

    @Override
    public @NonNull <T> Flux<T> filter(@NonNull Flux<T> source,
                                       @NonNull AssetType assetType,
                                       @NonNull Function<T, ?> assetId,
                                       @NonNull AssetPermission... permission) {
        Flux<T> cache = source.cache();

        return Flux
            .merge(Flux.fromIterable(members).map(mem -> mem.filter(cache, assetType, assetId, permission)))
            .distinct()
            ;
    }

    @Override
    public @NonNull Mono<Boolean> hasPermission(@NonNull AssetType assetType,
                                                @NonNull Collection<?> assetId,
                                                boolean allowAssetNotExist,
                                                @NonNull AssetPermission... permission) {
        return Flux
            .fromIterable(members)
            .filterWhen(member -> member.hasPermission(assetType, assetId, allowAssetNotExist, permission))
            .hasElements();
    }

    @Override
    public @NonNull Mono<Boolean> hasPermission(@NonNull String assetType,
                                                @NonNull Collection<?> assetId,
                                                boolean allowAssetNotExist,
                                                @NonNull AssetPermission... permission) {
        return Flux
            .fromIterable(members)
            .filterWhen(member -> member.hasPermission(assetType, assetId, allowAssetNotExist, permission))
            .hasElements();
    }
}
