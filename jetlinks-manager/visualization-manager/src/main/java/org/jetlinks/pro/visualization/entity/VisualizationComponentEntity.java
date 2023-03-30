package org.jetlinks.pro.visualization.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.jetlinks.pro.visualization.enums.DataVisualizationState;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;

@Table(name = "vis_components", indexes = {
    @Index(name = "idx_vis_com_type", columnList = "type")
})
@Getter
@Setter
public class VisualizationComponentEntity extends GenericTreeSortSupportEntity<String> {

    @Column(nullable = false)
    private String name;

    //catalog,component
    @Column(length = 64, nullable = false)
    private String type;

    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private String data;

    @Column
    private String description;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    private DataVisualizationState state;

    private List<VisualizationComponentEntity> children;

}
