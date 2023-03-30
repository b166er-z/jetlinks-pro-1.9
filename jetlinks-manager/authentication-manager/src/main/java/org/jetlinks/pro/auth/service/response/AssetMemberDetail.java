package org.jetlinks.pro.auth.service.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.impl.entity.AssetMemberBindEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class AssetMemberDetail {

    @Schema(description = "成员资产关联ID")
    private String bindId;

    @Schema(description = "是否已绑定此资产")
    private boolean binding;

    @Schema(description = "资产ID")
    private String assetId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "资产类型")
    private AssetType assetType;

    @Schema(description = "资产权限信息")
    private List<PermissionInfo> permissions;

    public static AssetMemberDetail from(AssetMemberBindEntity entity, AssetType type) {
        AssetMemberDetail detail = new AssetMemberDetail();
        detail.setBindId(entity.getId());
        detail.setAssetId(entity.getAssetId());
        detail.setAssetType(type);
        detail.setUserId(entity.getUserId());
        detail.setBinding(true);

        detail.setPermissions(type.getPermissions()
            .stream()
            .map(permission -> PermissionInfo.from(entity.getPermission(), permission))
            .collect(Collectors.toList())
        );

        return detail;
    }

    public void with(TenantMemberEntity member){
        this.setUserId(member.getUserId());
        this.setUserName(member.getName());
    }

    public static AssetMemberDetail from(@Nonnull TenantMemberEntity member,
                                         @Nullable AssetMemberBindEntity bind,
                                         @Nonnull AssetType type,
                                         @Nonnull String assetId) {
        AssetMemberDetail detail = new AssetMemberDetail();
        if (bind != null) {
            detail.setBindId(bind.getId());
            detail.setBinding(true);
        }
        detail.setAssetId(assetId);
        detail.setAssetType(type);
        detail.with(member);
        long perm = bind == null ? 0 : bind.getPermission();
        detail.setPermissions(type.getPermissions()
            .stream()
            .map(permission -> PermissionInfo.from(perm, permission))
            .collect(Collectors.toList())
        );
        return detail;
    }

    @Getter
    @Setter
    public static class PermissionInfo {

        @Schema(description = "权限ID")
        private String id;

        @Schema(description = "权限名称")
        private String name;

        @Schema(description = "已授权")
        private boolean allowed;

        public static PermissionInfo from(long val, AssetPermission permission) {
            PermissionInfo info = new PermissionInfo();
            info.id = permission.getValue();
            info.name = permission.getText();
            info.allowed = permission.in(val);
            return info;
        }
    }
}
