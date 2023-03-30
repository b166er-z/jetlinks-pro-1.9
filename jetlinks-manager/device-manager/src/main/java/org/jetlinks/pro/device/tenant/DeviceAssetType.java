package org.jetlinks.pro.device.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.EnumAssetType;
import org.jetlinks.pro.tenant.supports.CrudAssetPermission;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum DeviceAssetType implements EnumAssetType {

    product("产品", Arrays.asList(CrudAssetPermission.values())),
    device("设备", Arrays.asList(CrudAssetPermission.values())),
    deviceGroup("设备分组", Arrays.asList(CrudAssetPermission.values())),
    protocol("编解码协议",Arrays.asList(CrudAssetPermission.values()))
    ;

    private final String name;

    private final List<AssetPermission> permissions;

    @Override
    public String getId() {
        return name();
    }

}
