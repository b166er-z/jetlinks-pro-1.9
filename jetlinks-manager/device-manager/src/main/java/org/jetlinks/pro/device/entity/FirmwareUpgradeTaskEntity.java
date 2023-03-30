package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.device.enums.FirmwareUpgradeMode;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * 固件升级任务
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
@Table(name = "dev_firmware_upgrade_task", indexes = {
    @Index(name = "idx_fir_task_firmware_id", columnList = "firmware_id")
})
public class FirmwareUpgradeTaskEntity extends GenericEntity<String> {

    @Column
    @Schema(description = "任务名称")
    private String name;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)")
    private Date createTime;

    @Column(length = 64, nullable = false)
    @NotBlank(message = "[productId]不能为空")
    @Schema(description = "产品ID")
    private String productId;

    @Column
    @Schema(description = "固件ID")
    @NotBlank(message = "[firmwareId]不能为空")
    private String firmwareId;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @Schema(description = "升级超时时间")
    private Long timeoutSeconds;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "升级方式")
    private FirmwareUpgradeMode mode;


}
