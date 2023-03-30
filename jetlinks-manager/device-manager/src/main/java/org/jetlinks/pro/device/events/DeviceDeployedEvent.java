package org.jetlinks.pro.device.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.event.DefaultAsyncEvent;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class DeviceDeployedEvent extends DefaultAsyncEvent {

    private final List<DeviceInstanceEntity> devices;

}
