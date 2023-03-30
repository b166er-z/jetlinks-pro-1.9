package org.jetlinks.pro.visualization.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.visualization.enums.DataVisualizationState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;

/**
 * 数据可视化
 *
 * @author zhouhao
 * @since 1.1
 */
@Table(name = "vis_data_visualization", indexes = {
    @Index(name = "idx_vis_type_target", columnList = "type,target"),
    @Index(name = "idx_vis_type_catalog", columnList = "type,catalog_id")
})
@EqualsAndHashCode(callSuper = true)
@Data
public class DataVisualizationEntity extends GenericEntity<String> {

    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[type]不能为空",groups = CreateGroup.class)
    private String type;

    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[target]不能为空",groups = CreateGroup.class)
    private String target;

    //分类ID
    @Column(length = 64)
    private String catalogId;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    private String metadata;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    private DataVisualizationState state;

    public void applyId() {
        if (StringUtils.isEmpty(target)) {
            this.target = IDGenerator.SNOW_FLAKE_STRING.generate();
        }
        if (StringUtils.isEmpty(this.getId())) {
            this.setId(DigestUtils.md5Hex(String.format("%s:%s", type, target)));
        }
    }

    public static DataVisualizationEntity newEmpty(String type, String target) {
        DataVisualizationEntity entity = new DataVisualizationEntity();
        entity.setType(type);
        entity.setTarget(target);
        entity.applyId();
        entity.setMetadata("");
        return entity;
    }
}
