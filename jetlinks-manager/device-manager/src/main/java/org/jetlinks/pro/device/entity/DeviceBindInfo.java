package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DeviceBindInfo {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "类型")
    private String bindType;

    @Schema(description = "名称")
    private String bindName;

    @Schema(description = "第三方设备唯一标识")
    private String bindKey;

    @Schema(description = "描述")
    private String description;
}
