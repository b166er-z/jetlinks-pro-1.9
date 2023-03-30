package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_protocol")
public class ProtocolSupportEntity extends GenericEntity<String> {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(
        regexp = "^[0-9a-zA-Z_\\-]+$",
        message = "ID只能由数字,字母,下划线和中划线组成",
        groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    @Column
    @Schema(description = "协议名称")
    private String name;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @Schema(description = "类型")
    private String type;

    @Column
    @Schema(description = "状态")
    private Byte state;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "配置")
    private Map<String, Object> configuration;

    public ProtocolSupportDefinition toUnDeployDefinition() {
        ProtocolSupportDefinition definition = toDeployDefinition();
        definition.setState((byte) 0);
        return definition;
    }

    public ProtocolSupportDefinition toDeployDefinition() {
        ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
        definition.setId(getId());
        definition.setConfiguration(configuration);
        definition.setName(name);
        definition.setProvider(type);
        definition.setState((byte) 1);

        return definition;
    }
}
