package org.jetlinks.pro.network.manager.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.EnumAssetType;
import org.jetlinks.pro.tenant.supports.CrudAssetPermission;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum NetworkAssetType implements EnumAssetType {

    certificate("CA证书", Arrays.asList(CrudAssetPermission.values())),
    network("网络组件", Arrays.asList(CrudAssetPermission.values())),
    deviceGateway("设备接入网关", Arrays.asList(CrudAssetPermission.values()))
    ;

    private final String name;

    private final List<AssetPermission> permissions;

    @Override
    public String getId() {
        return name();
    }

}
