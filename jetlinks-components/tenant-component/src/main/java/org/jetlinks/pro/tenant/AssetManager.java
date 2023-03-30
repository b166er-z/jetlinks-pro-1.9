package org.jetlinks.pro.tenant;

import org.jetlinks.pro.tenant.impl.BindAssetsRequest;
import org.jetlinks.pro.tenant.impl.UnbindAssetsRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 租户资产管理器
 *
 * @author zhouhao
 * @since 1.3
 */
public interface AssetManager {

    /**
     * 根据资产类型ID获取资产类型,如果类型不存在，则返回{@link Mono#empty()}
     *
     * @param typeId ID
     * @return 资产类型
     */
    Mono<AssetType> getAssetType(@Nonnull String typeId);

    /**
     * 获取租户成员资产
     *
     * @param tenantId  租户ID
     * @param assetType 资产类型
     * @param userId    成员用户ID
     * @param assetId   资产ID集合
     * @return 资产集合
     */
    Flux<TenantAsset> getTenantAssets(@Nonnull String tenantId,
                                      @Nonnull AssetType assetType,
                                      @Nullable String userId,
                                      @Nonnull Collection<?> assetId);

    /**
     * 获取租户成员全部资产
     *
     * @param tenantId  租户ID
     * @param assetType 资产类型
     * @param userId    成员用户Id
     * @return 资产集合
     */
    Flux<TenantAsset> getTenantAssets(@Nonnull String tenantId,
                                      @Nonnull AssetType assetType,
                                      @Nullable String userId);

    /**
     * 获取租户成员单个资产
     *
     * @param tenantId  租户ID
     * @param assetType 资产类型
     * @param userId    成员用户ID
     * @param assetId   资产ID
     * @return 资产
     */
    Mono<TenantAsset> getTenantAsset(@Nonnull String tenantId,
                                     @Nonnull AssetType assetType,
                                     @Nullable String userId,
                                     @Nonnull String assetId);

    /**
     * 解绑资产
     *
     * @param tenantId      租户ID
     * @param requestStream 请求流
     * @return 解绑数量
     */
    Mono<Integer> unbindAssets(@Nonnull String tenantId,
                               @Nonnull Flux<UnbindAssetsRequest> requestStream);

    /**
     * 绑定资产
     *
     * @param tenantId           租户ID
     * @param filterOutsideAsset 是否过滤非当前租户的资产
     * @param requestStream      请求流
     * @return 绑定数量
     */
    Mono<Integer> bindAssets(@Nonnull String tenantId,
                             boolean filterOutsideAsset,
                             @Nonnull Flux<BindAssetsRequest> requestStream);

    /**
     * 绑定资产给用户
     *
     * @param tenantId  租户ID
     * @param assetType 资产类型
     * @param userId    成员用户ID
     * @param assetId   资产ID集合
     * @return void
     */
    Mono<Void> bindAssetsOwner(@Nonnull String tenantId,
                               @Nonnull AssetType assetType,
                               @Nonnull String userId,
                               @Nonnull Collection<?> assetId);

    /**
     * 解绑租户下用户的资产
     *
     * @param tenantId  租户ID
     * @param assetType 资产类型
     * @param userId    成员用户Id
     * @param assetId   资产Id
     * @return void
     */
    Mono<Void> unbindAssetsOwner(@Nonnull String tenantId,
                                 @Nonnull AssetType assetType,
                                 @Nonnull String userId,
                                 @Nonnull Collection<?> assetId);

    /**
     * 获取单个资产
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return 资产
     */
    Mono<Asset> getAsset(@Nonnull AssetType assetType,
                         @Nonnull String assetId);

    /**
     * 获取资产
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return 资产集合
     */
    Flux<Asset> getAssets(@Nonnull AssetType assetType,
                          @Nonnull Collection<?> assetId);

    /**
     * 获取资产对应的租户资产信息
     *
     * @param assetType 资产类型
     * @param assetId   资产ID集合
     * @return 租户资产
     */
    Flux<TenantAsset> getTenantAssets(@Nonnull AssetType assetType,
                                      @Nonnull Collection<?> assetId);

}
