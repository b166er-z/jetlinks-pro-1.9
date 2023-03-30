package org.jetlinks.pro.device.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.event.DefaultAsyncEvent;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class LedDeployedEvent extends DefaultAsyncEvent {

    private final Flux<DeviceInstanceEntity> devices;

}
