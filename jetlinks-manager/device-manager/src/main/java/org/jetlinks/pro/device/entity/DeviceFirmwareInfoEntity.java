package org.jetlinks.pro.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Date;
import java.util.Map;

/**
 * 设备固件信息
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
@Table(name = "dev_firmware_info", indexes = {
    @Index(name = "idx_dev_fir_inf_product_id", columnList = "product_id")
})
public class DeviceFirmwareInfoEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false)
    private String deviceName;

    @Column(length = 64, updatable = false, nullable = false)
    private String productId;

    @Column(length = 32)
    private String version;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Date createTime;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Date updateTime;

    @JsonCodec
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    private Map<String, Object> properties;

}
