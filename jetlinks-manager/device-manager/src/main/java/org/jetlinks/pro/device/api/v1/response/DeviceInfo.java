package org.jetlinks.pro.device.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.enums.DeviceState;

import java.io.Serializable;
import java.util.Optional;

@Getter
@Setter
public class DeviceInfo implements Serializable {

    @Getter
    @Setter
    @Schema(description = "设备ID")
    private String id;

    @Getter
    @Setter
    @Schema(description = "设备名称")
    private String name;

    @Getter
    @Setter
    @Schema(description = "产品ID")
    private String productId;

    @Getter
    @Setter
    @Schema(description = "产品名称")
    private String productName;

    @Getter
    @Setter
    @Schema(description = "设备状态")
    private DeviceState state;

    @Getter
    @Setter
    @Schema(description = "激活时间")
    private long registerTime;

    @Getter
    @Setter
    @Schema(description = "创建时间")
    private long createTime;

    @Getter
    @Setter
    @Schema(description = "父设备ID")
    private String parentId;

    public static DeviceInfo of(DeviceInstanceEntity entity) {
        DeviceInfo info = FastBeanCopier.copy(entity, new DeviceInfo());

        Optional.ofNullable(entity.getRegistryTime())
            .ifPresent(info::setRegisterTime);
        return info;
    }
}
