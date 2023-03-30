package org.jetlinks.pro.device.message.writer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import reactor.core.publisher.Mono;

/**
 * 用于将设备消息写入到时序数据库
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
@AllArgsConstructor
public class TimeSeriesMessageWriterConnector {


    private final DeviceDataService dataService;


    @Subscribe(topics = "/device/**", id = "device-message-ts-writer")
    public Mono<Void> writeDeviceMessageToTs(DeviceMessage message) {
        return dataService.saveDeviceMessage(message);
    }


}
