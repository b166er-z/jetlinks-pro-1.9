package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.device.entity.DevicePropertiesEntity;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/device")
@Slf4j
@Authorize
@Resource(id = "device-instance", name = "设备实例")
@DeviceAsset
@Tag(name = "设备指令API")
@Deprecated
public class DeviceMessageController {

    @Autowired
    public LocalDeviceInstanceService instanceService;

    //获取设备属性
    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @QueryAction
    @Deprecated
    public Flux<?> getProperty(@PathVariable String deviceId, @PathVariable String property) {
        return instanceService
            .readProperty(deviceId, property)
            .flux();

    }

    //获取标准设备属性
    @GetMapping("/standard/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @QueryAction
    @Deprecated
    public Mono<DeviceProperty> getStandardProperty(@PathVariable String deviceId, @PathVariable String property) {
        return instanceService.readAndConvertProperty(deviceId, property);

    }

    //设置设备属性
    @PostMapping("/setting/{deviceId}/property")
    @SneakyThrows
    @QueryAction
    @Deprecated
    public Flux<?> writeProperties(@PathVariable String deviceId, @RequestBody Mono<Map<String, Object>> properties) {
        return properties.flatMapMany(props -> instanceService.writeProperties(deviceId, props));
    }

    //设备功能调用
    @PostMapping("invoked/{deviceId}/function/{functionId}")
    @SneakyThrows
    @QueryAction
    @Deprecated
    public Flux<?> invokedFunction(@PathVariable String deviceId,
                                   @PathVariable String functionId,
                                   @RequestBody Mono<Map<String, Object>> properties) {

        return properties.flatMapMany(props -> instanceService.invokeFunction(deviceId, functionId, props));


    }

    //获取设备所有属性
    @PostMapping("/{deviceId}/properties")
    @SneakyThrows
    @QueryAction
    @Deprecated
    public Flux<?> getProperties(@PathVariable String deviceId,
                                 @RequestBody Flux<String> properties) {

        return properties.collectList().flatMapMany(list -> instanceService.readProperties(deviceId, list));
    }

}
