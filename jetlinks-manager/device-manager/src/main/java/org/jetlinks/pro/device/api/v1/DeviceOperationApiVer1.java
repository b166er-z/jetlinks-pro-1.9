package org.jetlinks.pro.device.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@Resource(id = "device-opt-api", name = "设备操作API")
@RequestMapping("/api/v1/device")
@DeviceAsset
@Tag(name = "设备操作API")
public class DeviceOperationApiVer1 {

    @Autowired
    private LocalDeviceInstanceService instanceService;

    /**
     * 获取设备属性
     * <pre>
     *     GET /api/v1/device/{deviceId}/property/{propertyId}
     *
     * </pre>
     */
    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @ResourceAction(id = "read-property", name = "读取属性")
    @Operation(summary = "下发读取属性指令", description = "向设备下发读取属性指令(ReadPropertyMessage)并获取返回.")
    public Mono<Map<String, Object>> getProperty(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                 @PathVariable @Parameter(description = "属性ID") String property) {

        return instanceService.readProperty(deviceId, property);

    }

    /**
     * 设置设备属性
     *
     * <pre>
     *     POST /api/v1/device/{deviceId}/properties
     *
     *     {
     *         "interval":"1s"
     *     }
     *
     * </pre>
     */
    @PostMapping("/{deviceId}/properties")
    @SneakyThrows
    @ResourceAction(id = "write-property", name = "修改属性")
    @Operation(summary = "下发修改属性指令", description = "向设备下发修改属性指令(WritePropertyMessage)并获取返回.")
    public Mono<Map<String, Object>> writeProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                     @RequestBody Mono<Map<String, Object>> properties) {
        return properties.flatMap(props -> instanceService.writeProperties(deviceId, props));
    }

    /**
     * 设备功能调用
     *
     * <pre>
     *     POST /api/v1/device/{deviceId}/function/{functionId}
     *
     *     {
     *         "arg0":"",
     *         "arg1":""
     *     }
     *
     *     响应:
     *     {
     *      "success":true,
     *      "result":[  //可能存在一次请求，分片响应，所以使用集合.
     *          response1,
     *          response2
     *      ]
     *     }
     * </pre>
     */
    @PostMapping("/{deviceId}/function/{functionId:.+}")
    @SneakyThrows
    @ResourceAction(id = "invoke-function", name = "调用功能")
    @Operation(summary = "下发调用功能指令", description =
        "向设备下发调用功能指令(FunctionInvokeMessage)并获取返回." +
            "请求体为Json对象,key为功能参数标识,value为功能参数值.如: {\"arg0\":\"1\"}")
    public Flux<?> invokeFunction(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                  @PathVariable @Parameter(description = "设备ID") String functionId,
                                  @RequestParam(required = false, defaultValue = "true")
                                  @Parameter(description = "是否转换返回结果,如果为false,则返回FunctionInvokeMessageReply,否则返回功能输出.")
                                      boolean convert,
                                  @RequestBody @Parameter(description = "参数") Mono<Map<String, Object>> args) {

        return args.flatMapMany(props -> instanceService.invokeFunction(deviceId, functionId, props, convert));
    }

}
