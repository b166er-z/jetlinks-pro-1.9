package org.jetlinks.pro;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.message.HeaderKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author wangzheng
 * @since 1.0
 */
public interface PropertyConstants {
    //机构ID
    Key<String> orgId = Key.of("orgId");

    //设备名称
    Key<String> deviceName = Key.of("deviceName");

    //产品名称
    Key<String> productName = Key.of("productName");

    //产品ID
    Key<String> productId = Key.of("productId");

    //租户ID
    Key<List<String>> tenantId = Key.of("tenantId");

    //分组ID
    Key<List<String>> groupId = Key.of("groupId");

    //租户成员ID
    Key<List<String>> tenantMemberId = Key.of("members");


    @SuppressWarnings("all")
    static <T> Optional<T> getFromMap(ConfigKey<T> key, Map<String, Object> map) {
        return Optional.ofNullable((T) map.get(key.getKey()));
    }

    interface Key<V> extends ConfigKey<V>, HeaderKey<V> {

        @Override
        default Class<V> getType() {
            return ConfigKey.super.getType();
        }

        static <T> Key<T> of(String key) {
            return new Key<T>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public T getDefaultValue() {
                    return null;
                }
            };
        }

    }
}
