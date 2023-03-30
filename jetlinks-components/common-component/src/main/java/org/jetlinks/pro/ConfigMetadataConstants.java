package org.jetlinks.pro;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.MergeOption;
import org.jetlinks.core.metadata.types.StringType;

import java.util.Map;

/**
 * @see ConfigKey
 */
public interface ConfigMetadataConstants {

    //字符串相关配置
    ConfigKey<Long> maxLength = ConfigKey.of("maxLength", "字符串最大长度", Long.TYPE);
    ConfigKey<Boolean> isRichText = ConfigKey.of("isRichText", "是否为富文本", Boolean.TYPE);
    ConfigKey<Boolean> isScript = ConfigKey.of("isScript", "是否为脚本", Boolean.TYPE);

    ConfigKey<Boolean> allowInput = ConfigKey.of("allowInput", "允许输入", Boolean.TYPE);
    ConfigKey<Boolean> required = ConfigKey.of("required", "是否必填", Boolean.TYPE);

    ConfigKey<Boolean> virtual = ConfigKey.of("virtual", "是否开启虚拟属性", Boolean.TYPE);

    ConfigKey<Map> virtualRule = ConfigKey.of("virtualRule", "虚拟属性规则", Map.class);


    MergeOption.ExpandsMerge ignoreMergeVirtualProperty = MergeOption.ExpandsMerge
        .ignore(virtual.getKey(), virtualRule.getKey());

    //在合并物模型时,移除虚拟属性配置.虚拟属性只支持产品配置
    MergeOption.ExpandsMerge removeVirtualProperty = MergeOption.ExpandsMerge
        .remove(virtual.getKey(), virtualRule.getKey());
}
