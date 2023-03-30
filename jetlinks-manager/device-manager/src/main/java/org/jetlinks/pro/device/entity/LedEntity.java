package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.device.enums.DeviceState;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "led_instance", indexes = {@Index(name = "idx_dev_led_name", columnList = "led_name"),@Index(name = "idx_dev_dtu_number", columnList = "dtu_number")})
public class LedEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId(){
        return super.getId();
    }

    @Column(name = "led_name", length = 64)
    @NotBlank(message = "Led名称不能为空", groups = CreateGroup.class)
    @Schema(description = "Led名称")
    private String name;

    @Column(name = "dtu_number")
    @NotBlank(message = "DTU编号不能为空", groups = CreateGroup.class)
    @Schema(description = "DTU编号")
    @DefaultValue("DTUNumber")
    private String dtu;

    @Column(name = "push")
    @Schema(description = "是否推送")
    @DefaultValue("0")
    private int push;

    @Column(name = "freq")
    @Schema(description = "推送频率。单位：分钟")
    @DefaultValue("10")
    private int freq;

    @Column(name = "metadata")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "LED模型配置")
    @DefaultValue("{}")
    private String metadata;

    @Column(name = "create_time", updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;

    @Column(name = "creator_id", updatable = false)
    @Schema(description = "创建者ID(只读)", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(name = "state", length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("notActive")
    @Schema(
        description = "状态(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
        , defaultValue = "notActive"
    )
    private DeviceState state;

    @Comment("产品id")
    @Column(name = "product_id", length = 64)
    @NotBlank(message = "产品ID不能为空", groups = CreateGroup.class)
    @Schema(description = "产品ID")
    private String productId;

    public static Mono<LedEntity> of(DeviceInstanceEntity de){
        LedEntity entity = new LedEntity();
        entity.setId(de.getId());
        entity.setName(de.getName());
        entity.setFreq(10);
        entity.setCreatorId(de.getCreatorId());
        entity.setState(DeviceState.notActive);
        entity.setProductId(de.getProductId());
        return Mono.just(entity);
    }

}
