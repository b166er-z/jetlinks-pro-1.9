package org.jetlinks.pro.tenant;

import java.util.List;

public interface TenantAsset {

    String getTenantId();

    String getAssetId();

    AssetType getAssetType();

    String getOwnerId();

    long getPermissionValue();

    default List<AssetPermission> getPermissions() {
        return getAssetType().getPermissions(getPermissionValue());
    }

    default boolean hasPermission(AssetPermission... permission) {
        return permission.length == 0 || getAssetType().hasPermission(getPermissionValue(), permission);
    }

}
