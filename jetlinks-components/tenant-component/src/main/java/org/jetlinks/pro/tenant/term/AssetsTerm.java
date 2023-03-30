package org.jetlinks.pro.tenant.term;


import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.pro.tenant.TenantMember;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 支持多种方式构建条件
 * <ul>
 *     <li>
 *          tenantId,assetType,memberId
 *     </li>
 *     <li>
 *         [tenantId,assetType,memberId]
 *     </li>
 *     <li>
 *         {"t":"tenantId","a":"assetType","m":"memberId"}
 *     </li>
 *     <li>
 *         {"tenantId":"tenantId","assetType":"assetType","memberId":"memberId"}
 *     </li>
 * </ul>
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class AssetsTerm {

    public static final String ID = "assets";

    //租户ID
    private String tenantId;

    //资产类型
    private String assetType;

    //成员(用户)ID
    private String memberId;

    //不在租户中
    private boolean not;

    @SuppressWarnings("all")
    public static AssetsTerm of(Term val) {

        Object value = val.getValue();
        if (value instanceof AssetsTerm) {
            return ((AssetsTerm) value);
        }

        if (value instanceof String) {
            String strVal = String.valueOf(value);
            if (strVal.startsWith("{") || strVal.startsWith("[")) {
                value = JSON.parse(strVal);
            } else {
                value = Arrays.asList(((String) value).split("[,]"));
            }
        }
        if (value instanceof Map) {
            return of(((Map) value));
        }
        if (value instanceof List) {
            List<String> arr = ((List<String>) value);
            if (arr.size() == 3) {
                return new AssetsTerm(arr.get(0), arr.get(1), arr.get(2), false);
            }
        }
        throw new IllegalArgumentException("不支持的查询格式");
    }


    public static AssetsTerm of(Map<String, Object> val) {

        AssetsTerm term = new AssetsTerm();
        term.tenantId = String.valueOf(Objects.requireNonNull(val.getOrDefault("tenantId", val.get("t")), "无参数[tenantId]"));
        term.assetType = String.valueOf(Objects.requireNonNull(val.getOrDefault("assetType", val.get("a")), "无参数[assetType]"));
        term.memberId = (String) val.getOrDefault("memberId", val.get("m"));
        term.not = "true".equals(String.valueOf(val.getOrDefault("not", val.get("n"))));

        return term;
    }

    public static AssetsTerm from(String type, TenantMember tenantMember) {
        return AssetsTerm.of(tenantMember.getTenant().getId(), type, tenantMember.isAdmin() ? null : tenantMember.getUserId(), false);
    }

    public static AssetsTerm from(String type, String tenantId) {
        return AssetsTerm.of(tenantId, type, null, false);
    }

}
