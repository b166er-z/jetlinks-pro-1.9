package org.jetlinks.pro.auth.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.auth.enums.TenantMemberState;
import org.jetlinks.pro.tenant.dimension.TenantDimensionType;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CreateMemberRequest {

    @NotBlank(message = "[name]不能为空")
    @Schema(description = "成员名称")
    private String name;

    @NotBlank(message = "[username]不能为空")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "[password]不能为空")
    @Schema(description = "密码")
    private String password;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "是否为管理员")
    private boolean admin;

    public TenantMemberEntity toMember() {
        TenantMemberEntity entity = new TenantMemberEntity();
        entity.setAdminMember(isAdmin());
        entity.setCreateTime(new Date());
        entity.setName(name);
        entity.setDescription(getDescription());
        entity.setState(TenantMemberState.enabled);
        entity.setType(TenantDimensionType.tenantMember);
        return entity;
    }
}
