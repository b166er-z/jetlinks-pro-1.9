package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.List;

@Getter
@Setter
@Table(name = "dev_product_category")
public class DeviceCategoryEntity extends GenericTreeSortSupportEntity<String> {

    @Override
    @Id
    @Column(length = 64, updatable = false)
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @NotBlank(message = "ID不能为空",groups = CreateGroup.class)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-|]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    public String getId() {
        return super.getId();
    }

    @Schema(description = "标识")
    @Column(nullable = false,length = 64)
    @NotBlank(message = "标识不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "分类标识只能由数字,字母,下划线和中划线组成")
    private String key;

    @Schema(description = "名称")
    @Column(nullable = false)
    @NotBlank
    private String name;

    @Schema(description = "说明")
    @Column
    private String description;

    @Schema(description = "子节点")
    private List<DeviceCategoryEntity> children;

    @Schema(description = "物模型")
    @Column
    @ColumnType(javaType = String.class, jdbcType = JDBCType.CLOB)
    private String metadata;
}
