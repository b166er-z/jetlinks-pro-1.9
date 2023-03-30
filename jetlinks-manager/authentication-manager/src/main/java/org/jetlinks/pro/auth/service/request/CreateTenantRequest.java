package org.jetlinks.pro.auth.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.auth.entity.TenantEntity;
import org.jetlinks.pro.auth.enums.TenantState;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class CreateTenantRequest {

    @NotBlank(message = "[name]不能为空")
    @Schema(description = "租户名称")
    private String name;

    @Schema(description = "租户图片地址")
    private String photo;

    @Schema(description = "租户类型")
    private String type;

    @Schema(description = "说明")
    private String description;

    @NotBlank(message = "[username]不能为空")
    @Schema(description = "租户管理员用户名")
    private String username;

    @NotBlank(message = "[password]不能为空")
    @Schema(description = "租户管理员密码")
    private String password;

    public TenantEntity toTenantEntity() {
        TenantEntity entity = new TenantEntity();

        entity.setName(name);
        entity.setState(TenantState.enabled);
        entity.setDescription(description);
        entity.setPhoto(photo);
        entity.setType(type);

        return entity;


    }

}
