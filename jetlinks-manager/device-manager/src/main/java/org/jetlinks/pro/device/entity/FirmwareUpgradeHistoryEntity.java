package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.device.enums.FirmwareUpgradeState;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

/**
 * 固件升级记录
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
@Table(name = "dev_firmware_upgrade_history", indexes = {
    @Index(name = "idx_dev_fir_his_device_id", columnList = "device_id"),
    @Index(name = "idx_dev_fir_his_task_id", columnList = "task_id")
})
public class FirmwareUpgradeHistoryEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "设备ID")
    private String deviceId;

    @Column(length = 64)
    @Schema(description = "设备名称")
    private String deviceName;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "产品ID")
    private String productId;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "固件ID")
    private String firmwareId;

    @Column(length = 32, nullable = false)
    @Schema(description = "固件版本")
    private String version;

    @Column(nullable = false)
    @Schema(description = "固件版本序号")
    private Integer versionOrder;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "升级任务ID")
    private String taskId;

    @Column(length = 32)
    @Schema(description = "升级任务名称")
    private String taskName;

    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)")
    private Date createTime;

    @Column
    @Schema(description = "升级时间")
    private Date upgradeTime;

    @Column
    @Schema(description = "完成时间")
    private Date completeTime;

    @Column
    @DefaultValue("0")
    @Schema(description = "升级超时时间")
    private Long timeoutSeconds;

    @Column
    @Schema(description = "升级进度")
    private Integer progress;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "升级状态")
    private FirmwareUpgradeState state;

    //失败原因
    @Column(length = 1024)
    @Schema(description = "失败原因")
    private String errorReason;

    public static FirmwareUpgradeHistoryEntity newInstance() {
        FirmwareUpgradeHistoryEntity entity = new FirmwareUpgradeHistoryEntity();
        entity.setCreateTime(new Date());
        entity.setState(FirmwareUpgradeState.waiting);
        entity.setProgress(0);

        return entity;
    }

    public FirmwareUpgradeHistoryEntity deviceId(String deviceId) {
        setDeviceId(deviceId);

        return this;
    }

    public FirmwareUpgradeHistoryEntity device(DeviceInstanceEntity instance) {
        setDeviceId(instance.getId());
        setDeviceName(instance.getName());
        return this;
    }

    public FirmwareUpgradeHistoryEntity with(FirmwareEntity firmwareEntity) {
        setFirmwareId(firmwareEntity.getId());
        setVersion(firmwareEntity.getVersion());
        setVersionOrder(firmwareEntity.getVersionOrder());

        return this;
    }

    public FirmwareUpgradeHistoryEntity with(FirmwareUpgradeTaskEntity taskEntity) {

        setTimeoutSeconds(taskEntity.getTimeoutSeconds());
        setTaskId(taskEntity.getId());
        setTaskName(taskEntity.getName());
        setProductId(taskEntity.getProductId());
        return this;
    }
}
