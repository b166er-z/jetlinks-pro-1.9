package org.jetlinks.pro.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.auth.enums.TenantMemberState;
import org.jetlinks.pro.auth.enums.TenantState;
import org.jetlinks.pro.tenant.dimension.TenantDimensionType;

/**
 * 租户成员详情
 * @author zhouhao
 * @see
 * @since 1.3
 */
@Getter
@Setter
public class TenantMemberDetail {

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "租户图片")
    private String tenantPhoto;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "成员类型")
    private TenantDimensionType type;

    @Schema(description = "是否为管理员")
    private boolean adminMember;

    @Schema(description = "是否为主租户")
    private boolean mainTenant;

    @Schema(description = "成员状态")
    private TenantMemberState state;

    @Schema(description = "租户状态")
    private TenantState tenantState;

    public static TenantMemberDetail of(TenantMemberEntity member, TenantEntity tenant) {
        TenantMemberDetail detail = new TenantMemberDetail();
        detail.setAdminMember(member.getAdminMember());
        detail.setMainTenant(member.getMainTenant());
        detail.setState(member.getState());
        detail.setTenantId(member.getTenantId());
        detail.setUserId(member.getUserId());
        detail.setType(member.getType());

        if (tenant != null) {
            detail.setTenantName(tenant.getName());
            detail.setTenantPhoto(tenant.getPhoto());
            detail.setTenantState(tenant.getState());
        }

        return detail;
    }
}
