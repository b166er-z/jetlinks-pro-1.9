package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.core.device.manager.BindInfo;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;

@Table(name = "dev_third_bind", indexes = {
    @Index(name = "idx_dev_bind_device_id", columnList = "type,device_id")
})
@Getter
@Setter
public class DeviceBindEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false)
    @Schema(description = "绑定类型,通常为第三方标识")
    private String type;

    @Column(length = 64, nullable = false)
    @Schema(description = "绑定键,通常为第三方设备ID")
    private String key;

    @Column(length = 64, nullable = false)
    @Schema(description = "设备ID,平台的设备ID")
    private String deviceId;

    @Column(length = 128)
    @Schema(description = "说明")
    private String description;

    public static DeviceBindEntity of(String type,String key,String deviceId,String description){
        DeviceBindEntity entity=new DeviceBindEntity();
        entity.setId(generateId(type,key));
        entity.setType(type);
        entity.setKey(key);
        entity.setDeviceId(deviceId);
        entity.setDescription(description);
        return entity;
    }

    public BindInfo toBindInfo(){
        BindInfo bindInfo=new BindInfo();
        bindInfo.setDeviceId(deviceId);
        bindInfo.setKey(key);
        bindInfo.setDescription(description);
        return bindInfo;
    }

    public String generateId() {
        this.setId(generateId(this.type, this.key));
        return getId();
    }

    public static String generateId(String type, String key) {

        return DigestUtils.md5Hex(String.format("%s-%s", type, key));
    }
}
