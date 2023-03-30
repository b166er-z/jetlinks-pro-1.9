package org.jetlinks.pro.tenant;

/**
 * 租户信息
 *
 * @author zhouhao
 * @since 1.3
 */
public interface Tenant {

    /**
     * @return 租户ID
     */
    String getId();

    /**
     * @return 租户名称
     */
    String getName();

}
