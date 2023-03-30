package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Getter
@Setter
public class DeviceGroupInfo {

    @Schema(description = "分组ID")
    private String id;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "分组下的设备信息")
    private List<DeviceBasicInfo> devices;

    public static DeviceGroupInfo of(DeviceGroupEntity group) {
        return of(group,new ArrayList<>());
    }
    public static DeviceGroupInfo of(DeviceGroupEntity group, List<DeviceInstanceEntity> instance) {

        DeviceGroupInfo info = FastBeanCopier.copy(group, new DeviceGroupInfo());

        info.setDevices(
            instance.stream()
                .map(DeviceBasicInfo::of)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new))
        );
        return info;

    }

}
