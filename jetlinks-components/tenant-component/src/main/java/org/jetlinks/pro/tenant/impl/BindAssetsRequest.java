package org.jetlinks.pro.tenant.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.pro.tenant.AssetManager;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.impl.entity.AssetMemberBindEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindAssetsRequest {

    @NotBlank
    @Schema(description = "用户ID")
    private String userId;

    @NotBlank
    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "资产ID集合")
    private List<String> assetIdList;

    @Schema(description = "授权信息")
    private List<String> permission;

    @Schema(description = "是否赋予全部权限")
    private boolean allPermission;

    public Flux<AssetMemberBindEntity> toBindEntity(String tenantId, AssetManager manager) {
        if (CollectionUtils.isEmpty(assetIdList)) {
            return Flux.empty();
        }
        return manager
            .getAssetType(assetType)
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("不支持的资产类型:" + assetType)))
            .flatMapMany(type ->
                Flux.fromIterable(assetIdList)
                    .map(assetId -> {
                        List<AssetPermission> assetPermissions = allPermission
                            ? type.getPermissions()
                            : type.getPermissions(permission);

                        AssetMemberBindEntity bindEntity = new AssetMemberBindEntity();
                        bindEntity.setAssetId(assetId);
                        bindEntity.setAssetType(assetType);
                        bindEntity.setPermission(type.createPermission(assetPermissions));
                        bindEntity.setUserId(userId);
                        bindEntity.setTenantId(tenantId);
                        bindEntity.generateId();
                        return bindEntity;
                    }));

    }
}