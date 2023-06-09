package org.jetlinks.pro.device.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.device.enums.DeviceState;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DeviceStateInfo {
    private String deviceId;

    private DeviceState state;
}
