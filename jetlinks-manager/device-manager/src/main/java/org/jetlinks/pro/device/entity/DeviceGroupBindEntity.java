package org.jetlinks.pro.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "dev_device_group_bind", indexes = {
    @Index(name = "idx_dev_group_group_id", columnList = "group_id"),
    @Index(name = "idx_dev_group_device_id", columnList = "device_id")
})
@Getter
@Setter
public class DeviceGroupBindEntity extends GenericEntity<String> {

    @Column(length = 64, updatable = false)
    private String groupId;

    @Column(length = 64, updatable = false)
    private String deviceId;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Date bindTime;

    public static DeviceGroupBindEntity of(String groupId, String deviceId) {
        DeviceGroupBindEntity entity = new DeviceGroupBindEntity();
        entity.setId(DigestUtils.md5Hex(groupId + deviceId));
        entity.setGroupId(groupId);
        entity.setDeviceId(deviceId);
        entity.setBindTime(new Date());
        return entity;
    }
}
