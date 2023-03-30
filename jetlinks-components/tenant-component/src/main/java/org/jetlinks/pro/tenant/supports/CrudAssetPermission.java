package org.jetlinks.pro.tenant.supports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.tenant.AssetPermission;

/**
 * 增删改查权限,请勿修改枚举顺序
 *
 * @author zhouhao
 * @since 1.2
 */
@Getter
@AllArgsConstructor
public enum CrudAssetPermission implements AssetPermission {

    read("查询"),
    save("保存"),
    delete("删除");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }


}
