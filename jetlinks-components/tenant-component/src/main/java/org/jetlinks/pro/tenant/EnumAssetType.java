package org.jetlinks.pro.tenant;

import org.hswebframework.web.dict.EnumDict;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface EnumAssetType extends AssetType, EnumDict<String> {

    @Override
    List<AssetPermission> getPermissions();

    @Override
    default List<AssetPermission> getPermissions(long value) {
        return EnumDict.getByMask(this::getPermissions, value);
    }

    @Override
    default boolean hasPermission(long value, AssetPermission[] permissions) {
        return EnumDict.maskInAny(value, permissions);
    }

    @Override
    default long createPermission(AssetPermission[] permissions) {
        return EnumDict.toMask(permissions);
    }

    @Override
    default long createAllPermission() {
        return createPermission(getPermissions().toArray(new AssetPermission[0]));
    }

    @Override
    default List<AssetPermission> getPermissions(Collection<String> value) {
        if (CollectionUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        Map<String, AssetPermission> mapping = getPermissions()
            .stream()
            .collect(Collectors.toMap(AssetPermission::getValue, Function.identity()));

        return value.stream()
            .map(mapping::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    default String getValue() {
        return getId();
    }

    @Override
    default String getText() {
        return getName();
    }
}
