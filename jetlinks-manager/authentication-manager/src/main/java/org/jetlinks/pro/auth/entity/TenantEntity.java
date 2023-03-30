package org.jetlinks.pro.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.auth.enums.TenantState;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.Date;

/**
 * 租户实体
 * @author zhouhao
 * @see GenericEntity
 * @since 1.3
 */
@Table(name = "s_tenant")
@Getter
@Setter
public class TenantEntity extends GenericEntity<String> {
    private static final long serialVersionUID = -7163490373702776998L;

    @Column(nullable = false)
    @Schema(description = "租户名称")
    private String name;

    @Column
    @Schema(description = "租户类型")
    private String type;

    @Column(length = 2000)
    @Schema(description = "租户图片地址")
    private String photo;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态")
    private TenantState state;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)")
    private Date createTime;
}
