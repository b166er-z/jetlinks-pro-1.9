package org.jetlinks.pro.authorize;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.DimensionType;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@AllArgsConstructor
@Getter
public enum OrgDimensionType implements DimensionType {
    org("机构");

    private final String name;

    @Override
    public String getId() {
        return name();
    }
}
