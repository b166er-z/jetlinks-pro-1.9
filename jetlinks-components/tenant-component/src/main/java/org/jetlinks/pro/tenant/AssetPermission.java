package org.jetlinks.pro.tenant;

import org.hswebframework.web.dict.EnumDict;

/**
 * 资产权限,单个资产的权限不能超过64个.
 * 建议使用枚举实现此接口
 *
 * @author zhouhao
 * @since 1.2
 */
public interface AssetPermission extends EnumDict<String> {

}
