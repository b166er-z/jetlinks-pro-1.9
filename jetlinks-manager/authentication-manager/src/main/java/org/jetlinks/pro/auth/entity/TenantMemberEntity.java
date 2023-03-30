package org.jetlinks.pro.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.auth.enums.TenantMemberState;
import org.jetlinks.pro.tenant.dimension.TenantDimensionType;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

/**
 * 租户成员实体
 * @author zhouhao
 * @since 1.3
 */
@Table(name = "s_tenant_member", indexes = {
    @Index(name = "idx_tenant_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_tenant_user_id", columnList = "user_id")
})
@Getter
@Setter
public class TenantMemberEntity extends GenericEntity<String> {

    @Column(nullable = false, length = 64, updatable = false)
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(nullable = false, length = 64, updatable = false)
    @Schema(description = "用户ID")
    private String userId;

    @Column(nullable = false)
    @Schema(description = "用户名称")
    private String name;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("tenantMember")
    @Schema(description = "成员类型")
    private TenantDimensionType type;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @DefaultValue("false")
    @Schema(description = "是否为租户管理员")
    private Boolean adminMember;

    @Column
    @DefaultValue("true")
    @Schema(description = "是否为主租户")
    private Boolean mainTenant;

    @Column(updatable = false)
    @Schema(description = "创建时间(只读)")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Date createTime;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态")
    private TenantMemberState state;

    public String generateId() {
        this.setId(DigestUtils.md5Hex(String.format("%s|%s", tenantId, userId)));
        return getId();
    }
}
