package org.jetlinks.pro.device.message;

import lombok.AllArgsConstructor;
import org.jetlinks.pro.device.service.DeviceFirmwareService;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@AllArgsConstructor
public class DeviceFirmwarePublishProvider implements SubscriptionProvider {

    private final DeviceFirmwareService deviceFirmwareService;

    @Override
    public String id() {
        return "device-firmware-publisher";
    }

    @Override
    public String name() {
        return "推送设备固件更新";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/device-firmware/publish"
        };
    }

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {


        String taskId = request.getString("taskId").orElseThrow(() -> new IllegalArgumentException("参数[taskId]不能为空."));

        if (!request.getAuthentication()
            .hasPermission("device-firmware-manager", "publish")) {
            return Flux.just(Message.error(request.getId(), request.getTopic(), "没有权限:[device-firmware-manager.publish]"));
        }

        return deviceFirmwareService
            .publishTaskUpgrade(taskId)
            .map(response -> Message.success(request.getId(), request.getTopic(), response));
    }
}
