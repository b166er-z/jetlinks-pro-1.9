package org.jetlinks.pro.tenant;

import java.util.Collection;
import java.util.List;

public interface AssetType {

    String getId();

    String getName();

    List<AssetPermission> getPermissions();

    List<AssetPermission> getPermissions(long value);

    boolean hasPermission(long value, AssetPermission[] permissions);

    long createPermission(AssetPermission[] permissions);

    default long createPermission(Collection<AssetPermission> permissions) {
        return createPermission(permissions.toArray(new AssetPermission[0]));
    }

    long createAllPermission();

    List<AssetPermission> getPermissions(Collection<String> value);
}
