package org.jetlinks.pro.auth.service.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.auth.entity.TenantEntity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TenantDetail {

    @Schema(description = "租户信息")
    private TenantEntity tenant;

    @Schema(description = "租户下成员数量")
    private int members;

    public static TenantDetail of(TenantEntity tenant) {
        return new TenantDetail(tenant, 0);
    }

}
