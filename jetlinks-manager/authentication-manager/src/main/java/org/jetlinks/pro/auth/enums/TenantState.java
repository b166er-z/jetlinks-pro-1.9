package org.jetlinks.pro.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

/**
 * 租户状态
 * @author zhouhao
 * @see
 * @since 1.3
 */
@Getter
@AllArgsConstructor
public enum TenantState implements EnumDict<String> {

    enabled("正常"),
    disabled("已禁用");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

}
