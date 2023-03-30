package org.jetlinks.pro.tenant.dimension;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
public enum TenantDimensionType implements DimensionType, EnumDict<String> {
    tenant("租户"),
    tenantMember("租户成员"),
    tenantCustomer("租户客户");

    private final String text;

    @Override
    public String getValue() {
        return getId();
    }

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getName() {
        return getText();
    }

    @Override
    public boolean isWriteJSONObjectEnabled() {
        return false;
    }

    public static boolean any(DimensionType type){
        for (TenantDimensionType value : values()) {
            if(value.isSameType(type)){
                return true;
            }
        }
        return false;
    }
}
