package org.jetlinks.pro.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.jetlinks.pro.auth.enums.TenantState;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息详情
 * @author zhouhao
 * @see
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class UserDetail {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "email")
    private String email;

    @Schema(description = "联系电话")
    private String telephone;

    @Schema(description = "头像图片地址")
    private String avatar;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "创建时间")
    private long createTime;

    @Schema(description = "租户信息")
    private List<TenantMemberDetail> tenants;

    private boolean tenantDisabled;

    public static UserDetail of(UserEntity entity) {
        return new UserDetail().with(entity);
    }

    public UserDetail with(List<TenantMemberDetail> details) {
        this.tenants = details
            .stream()
            .filter(detail -> {
                if (detail.getTenantState() != TenantState.enabled) {
                    tenantDisabled = true;
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        if (this.tenants.size() > 0) {
            tenantDisabled = false;
        }
        return this;
    }

    public UserDetail with(UserDetailEntity entity) {
        this.setAvatar(entity.getAvatar());
        this.setDescription(entity.getDescription());
        this.setTelephone(entity.getTelephone());
        this.setEmail(entity.getEmail());

        return this;
    }

    public UserDetail with(UserEntity entity) {
        this.setId(entity.getId());
        this.setName(entity.getName());
        if (entity.getCreateTime() != null) {
            setCreateTime(entity.getCreateTime());
        }
        return this;
    }

}
