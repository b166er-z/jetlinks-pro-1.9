package org.jetlinks.pro.notify.manager.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.EnumAssetType;
import org.jetlinks.pro.tenant.supports.CrudAssetPermission;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum NotifyAssetType implements EnumAssetType {

    notifyConfig("通知配置", Arrays.asList(CrudAssetPermission.values())),
    notifyTemplate("通知模版", Arrays.asList(CrudAssetPermission.values())),
    ;

    private final String name;

    private final List<AssetPermission> permissions;

    @Override
    public String getId() {
        return name();
    }

}
