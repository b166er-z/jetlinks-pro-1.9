package org.jetlinks.pro.tenant.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.EnumAssetType;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor(staticName = "of")
@Getter
@Setter
@NoArgsConstructor
public class UndefinedAssetType implements EnumAssetType {

    private String id;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public List<AssetPermission> getPermissions() {
        return Collections.emptyList();
    }

    @Override
    public int ordinal() {
        return 0;
    }
}
