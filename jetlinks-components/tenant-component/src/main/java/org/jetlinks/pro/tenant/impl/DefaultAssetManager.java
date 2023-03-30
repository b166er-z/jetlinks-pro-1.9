package org.jetlinks.pro.tenant.impl;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.jetlinks.pro.tenant.*;
import org.jetlinks.pro.tenant.event.AssetsBindEvent;
import org.jetlinks.pro.tenant.event.AssetsUnBindEvent;
import org.jetlinks.pro.tenant.impl.entity.AssetMemberBindEntity;
import org.jetlinks.pro.tenant.supports.DefaultTenantAsset;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultAssetManager implements AssetManager, BeanPostProcessor {

    private final Map<String, AssetSupplier> assetSuppliers = new ConcurrentHashMap<>();

    private final Map<String, AssetType> typeMapping = new ConcurrentHashMap<>();

    private final ReactiveRepository<AssetMemberBindEntity, String> bindRepository;

    private final ApplicationEventPublisher eventPublisher;

    public DefaultAssetManager(ReactiveRepository<AssetMemberBindEntity, String> bindRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.bindRepository = bindRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<AssetType> getAssetType(@Nonnull String typeId) {
        return Mono
            .justOrEmpty(typeMapping.get(typeId))
            .defaultIfEmpty(UndefinedAssetType.of(typeId));
    }

    private TenantAsset convertBindToAsset(AssetMemberBindEntity bind, AssetType assetType) {
        return DefaultTenantAsset.of(bind.getTenantId(), bind.getAssetId(), assetType, bind.getUserId(), bind.getPermission());
    }

    @Override
    public Flux<TenantAsset> getTenantAssets(@Nonnull String tenantId,
                                             @Nonnull AssetType assetType,
                                             @Nullable String userId,
                                             @Nonnull Collection<?> assetId) {
        Assert.notNull(tenantId, "tenantId cannot be null");
        Assert.notNull(assetType, "assetType cannot be null");
        Assert.notNull(assetId, "assetId cannot be null");

        return bindRepository
            .createQuery()
            .where(AssetMemberBindEntity::getAssetType, assetType.getId())
            .and(AssetMemberBindEntity::getTenantId, tenantId)
            .and(AssetMemberBindEntity::getUserId, userId)
            .in(AssetMemberBindEntity::getAssetId, assetId)
            .fetch()
            .map(bind -> convertBindToAsset(bind, assetType));
    }

    @Override
    public Flux<TenantAsset> getTenantAssets(@Nonnull String tenantId,
                                             @Nonnull AssetType assetType,
                                             @Nullable String userId) {
        Assert.notNull(tenantId, "tenantId cannot be null");
        Assert.notNull(assetType, "assetType cannot be null");

        return bindRepository
            .createQuery()
            .where(AssetMemberBindEntity::getAssetType, assetType.getId())
            .and(AssetMemberBindEntity::getTenantId, tenantId)
            .and(AssetMemberBindEntity::getUserId, userId)
            .fetch()
            .map(bind -> convertBindToAsset(bind, assetType));
    }

    @Override
    public Mono<TenantAsset> getTenantAsset(@Nonnull String tenantId,
                                            @Nonnull AssetType assetType,
                                            @Nullable String userId,
                                            @Nonnull String assetId) {
        return this
            .getTenantAssets(tenantId, assetType, userId, Collections.singletonList(assetId))
            .singleOrEmpty();
    }

    @Override
    public Mono<Void> bindAssetsOwner(@Nonnull String tenantId,
                                      @Nonnull AssetType assetType,
                                      @Nonnull String userId,
                                      @Nonnull Collection<?> assetId) {
        Assert.notNull(tenantId, "tenantId cannot be null");
        Assert.notNull(assetType, "assetType cannot be null");
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(assetId, "assetId cannot be null");

        return bindAssets(tenantId, false, Flux.just(BindAssetsRequest.builder()
            .assetType(assetType.getId())
            .assetIdList(assetId.stream().map(String::valueOf).collect(Collectors.toList()))
            .allPermission(true)
            .userId(userId)
            .build()))
            .then();
    }

    @Transactional
    public Mono<Integer> unbindAssets(@Nonnull String tenantId,
                                      @Nonnull Flux<UnbindAssetsRequest> requestStream) {
        Assert.notNull(tenantId, "tenantId cannot be null");
        Assert.notNull(requestStream, "requestStream cannot be null");

        return requestStream
            .doOnNext(UnbindAssetsRequest::validate)
            .flatMap(request -> bindRepository
                .createDelete()
                .where(AssetMemberBindEntity::getTenantId, tenantId)
                .and(AssetMemberBindEntity::getUserId, request.getUserId())
                .and(AssetMemberBindEntity::getAssetType, request.getAssetType())
                .in(AssetMemberBindEntity::getAssetId, request.getAssetIdList())
                .execute()
                //推送解绑事件
                .flatMap(i -> AssetsUnBindEvent
                    .of(tenantId, request.getAssetType(), request.getUserId(), request.getAssetIdList())
                    .publish(eventPublisher)
                    .thenReturn(i))
            )
            .reduce(Math::addExact);
    }

    @Transactional
    public Mono<Integer> bindAssets(@Nonnull String tenantId,
                                    boolean filterOutsideAsset,
                                    @Nonnull Flux<BindAssetsRequest> requestStream) {
        Assert.notNull(tenantId, "tenantId cannot be null");
        Assert.notNull(requestStream, "requestStream cannot be null");

        return requestStream
            .flatMap(request -> request.toBindEntity(tenantId, this))
            .buffer(200)
            .flatMap(list -> {
                if (!filterOutsideAsset) {
                    return this.doBind(list);
                }
                return Flux
                    .fromIterable(list)
                    .collect(Collectors.groupingBy(
                        AssetMemberBindEntity::getAssetType,
                        Collectors.mapping(AssetMemberBindEntity::getAssetId, Collectors.toList())))
                    .flatMapIterable(Map::entrySet)
                    .flatMap(entry ->
                        bindRepository.createQuery()
                            .where(AssetMemberBindEntity::getTenantId, tenantId)
                            .and(AssetMemberBindEntity::getAssetType, entry.getKey())
                            .in(AssetMemberBindEntity::getAssetId, entry.getValue())
                            .fetch()
                            .map(bind -> bind.getAssetType() + ":" + bind.getAssetId())
                            .collectList())
                    .flatMap(insideAsset ->
                        doBind(list
                            .stream()
                            .filter(bind -> {
                                if (!insideAsset.contains(bind.getAssetType() + ":" + bind.getAssetId())) {
                                    log.warn("租户[{}]没有资产权限{}:{}", tenantId, bind.getAssetType(), bind.getAssetId());
                                    return false;
                                }
                                return true;
                            })
                            .collect(Collectors.toList())));
            })
            .reduce(Math::addExact);

    }

    protected Mono<Integer> doBind(List<AssetMemberBindEntity> binds) {
        if (CollectionUtils.isEmpty(binds)) {
            return Mono.just(0);
        }
        return bindRepository
            .save(binds)
            .flatMap(i -> Flux
                .fromIterable(binds)
                //推送资产绑定事件
                .flatMap(bind -> AssetsBindEvent
                    .of(
                        bind.getTenantId(),
                        bind.getAssetType(),
                        bind.getUserId(),
                        Collections.singleton(bind.getAssetId())
                    )
                    .publish(eventPublisher))
                .then(Mono.just(i.getTotal()))
            );
    }

    @Override
    public Mono<Void> unbindAssetsOwner(@Nonnull String tenantId,
                                        @Nonnull AssetType assetType,
                                        @Nonnull String userId,
                                        @Nonnull Collection<?> assetId) {
        return unbindAssets(tenantId, Flux.just(UnbindAssetsRequest.builder()
            .assetType(assetType.getId())
            .assetIdList(assetId.stream().map(String::valueOf).collect(Collectors.toList()))
            .userId(userId)
            .build()))
            .then();
    }

    @Override
    public Mono<Asset> getAsset(@Nonnull AssetType assetType,
                                @Nonnull String assetId) {
        return Mono
            .justOrEmpty(assetSuppliers.get(assetType.getId()))
            .flatMap(supplier -> supplier.getAssets(assetType, Collections.singletonList(assetId)).singleOrEmpty());
    }

    @Override
    public Flux<Asset> getAssets(@Nonnull AssetType assetType,
                                 @Nonnull Collection<?> assetId) {
        return Mono
            .justOrEmpty(assetSuppliers.get(assetType.getId()))
            .flatMapMany(supplier -> supplier.getAssets(assetType, assetId));
    }

    @Override
    public Flux<TenantAsset> getTenantAssets(@Nonnull AssetType assetType,
                                             @Nonnull Collection<?> assetId) {
        if (CollectionUtils.isEmpty(assetId)) {
            return Flux.empty();
        }
        return bindRepository
            .createQuery()
            .where(AssetMemberBindEntity::getAssetType, assetType.getId())
            .in(AssetMemberBindEntity::getAssetId, assetId)
            .fetch()
            .map(bind -> convertBindToAsset(bind, assetType));
    }

    protected void register(AssetSupplier supplier) {
        for (AssetType type : supplier.getTypes()) {
            assetSuppliers.put(type.getId(), supplier);
            typeMapping.put(type.getId(), type);
        }
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof AssetSupplier) {
            register(((AssetSupplier) bean));
        }
        return bean;
    }
}
