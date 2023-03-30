package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.manager.DeviceBindManager;
import org.jetlinks.pro.device.entity.DeviceOperationLogEntity;
import org.jetlinks.pro.device.entity.LedEntity;
import org.jetlinks.pro.device.entity.LedLog;
import org.jetlinks.pro.device.entity.LedTaskModel;
import org.jetlinks.pro.device.service.LocalLedInstanceService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/led")
@Authorize
@Resource(id = "led", name = "led大屏")
@Slf4j
@Tag(name = "led大屏接口")
public class LedController implements TenantAccessCrudController<LedEntity,String> {

    @Getter
    private final LocalLedInstanceService service;

    private final DeviceRegistry registry;

    private final DeviceBindManager deviceBindManager;

    @SuppressWarnings("all")
    public LedController(
        LocalLedInstanceService service,
        DeviceRegistry registry,
        DeviceBindManager deviceBindManager
    ){
        this.service = service;
        this.registry = registry;
        this.deviceBindManager = deviceBindManager;
    }

    /**
     * 修改指定led
     * @param id led大屏设备ID
     * @return 实际修改的数据条数
     */
    @PostMapping("/{id:.+}/update")
    @QueryAction
    @Operation(summary = "修改指定ID的LED设备")
    public Mono<Integer> updateLed(@PathVariable @Parameter(description = "LED设备ID") String id,
                                   @RequestBody Mono<LedEntity> ledEntity){
        return service.updateLed(id,ledEntity);
    }

    /**
     * 获取指定led详情
     * @param id led大屏设备ID
     * @return 大屏设备详情
     */
    @GetMapping("/{id:.+}/detail")
    @QueryAction
    @Operation(summary = "获取指定ID的LED设备详情")
    public Mono<LedEntity> getDeviceDetailInfo(@PathVariable @Parameter(description = "LED设备ID") String id){
        return service.findById(id);
    }


    //查询设备日志
    @GetMapping("/{ledId:.+}/logs")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "(GET)查询设备日志数据")
    public Mono<PagerResult<LedLog>> queryDeviceLog(
        @PathVariable @Parameter(description = "LED设备ID") String ledId,
        @Parameter(hidden = true) QueryParamEntity param
    ){
        return service.readLog(ledId,param);
    }

    //查询设备日志
    //    @PostMapping("/{deviceId:.+}/logs")
    //    @QueryAction
    //    @DeviceAsset(ignoreQuery = true)
    //    @Operation(summary = "(POST)查询设备日志数据")
    //    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(
    //        @PathVariable @Parameter(description = "设备ID") String deviceId,@Parameter(hidden = true) Mono<QueryParamEntity> queryParam
    //    ){
    //        return queryParam.flatMap(param -> deviceDataService.queryDeviceMessageLog(deviceId,param));
    //    }


//    //设置设备属性
//    @PutMapping("/{deviceId:.+}/property")
//    @SneakyThrows
//    @QueryAction
//    @Operation(summary = "发送设置属性指令到设备", description = "请求示例: {\"属性ID\":\"值\"}")
//    public Flux<?> writeProperties(
//        @PathVariable @Parameter(description = "设备ID") String deviceId,@RequestBody Mono<Map<String,Object>> properties
//    ){
//
//        return properties.flatMapMany(props -> service.writeProperties(deviceId,props));
//    }
//
//    //发送设备指令
//    @PostMapping("/{deviceId:.+}/message")
//    @SneakyThrows
//    @QueryAction
//    @Operation(summary = "发送指令到设备")
//    @SuppressWarnings("all")
//    public Flux<?> sendMessage(
//        @PathVariable @Parameter(description = "设备ID") String deviceId,@RequestBody Mono<Map<String,Object>> properties
//    ){
//        return properties.flatMapMany(props -> {
//            return Mono.zip(registry.getDevice(deviceId).map(DeviceOperator :: messageSender).switchIfEmpty(Mono.error(() -> new NotFoundException("设备不存在或未激活"))),Mono.<Message>justOrEmpty(MessageType.convertMessage(props)).cast(DeviceMessage.class).switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的消息格式")))).flatMapMany(tp2 -> {
//                DeviceMessageSender sender = tp2.getT1();
//                DeviceMessage message = tp2.getT2();
//
//                Map<String,String> copy = new HashMap<>();
//                copy.put("deviceId",deviceId);
//                if(!StringUtils.hasText(message.getMessageId())){
//                    copy.put("messageId",IDGenerator.SNOW_FLAKE_STRING.generate());
//                }
//                FastBeanCopier.copy(copy,message);
//                return sender.send(message).onErrorResume(DeviceOperationException.class,error -> {
//                    if(message instanceof RepayableDeviceMessage){
//                        return Mono.just(((RepayableDeviceMessage)message).newReply().error(error));
//                    }
//                    return Mono.error(error);
//                });
//            });
//        });
//    }

//    //发送设备指令
//    @PostMapping("/messages")
//    @SneakyThrows
//    @QueryAction
//    @Operation(summary = "批量发送指令到设备")
//    @SuppressWarnings("all")
//    @TenantAssets(ignore = true)
//    public Flux<?> sendMessage(
//        @RequestParam(required = false) @Parameter(description = "按查询条件发送指令") String where,@RequestBody Flux<Map<String,Object>> messages
//    ){
//
//        Lazy<Flux<DeviceOperator>> operators = Lazy.of(() -> {
//            if(StringUtils.isEmpty(where)){
//                throw new ValidationException("where","[where]参数不能为空");
//            }
//            QueryParamEntity entity = new QueryParamEntity();
//            entity.setWhere(where);
//            entity.includes("id");
//            //过滤租户数据
//            return TenantMember.injectQueryParam(entity,DeviceAssetType.device,"id").flatMapMany(service :: query).flatMap(device -> registry.getDevice(device.getId())).cache();
//        });
//        return messages.flatMap(message -> {
//            DeviceMessage msg = MessageType.convertMessage(message).filter(DeviceMessage.class :: isInstance).map(DeviceMessage.class :: cast).orElseThrow(() -> new UnsupportedOperationException("不支持的消息格式:" + message));
//
//            String deviceId = msg.getDeviceId();
//            Flux<DeviceOperator> devices = StringUtils.isEmpty(deviceId) ? operators.get() : registry.getDevice(deviceId).flux();
//
//            return devices.flatMap(device -> {
//                Map<String,Object> copy = new HashMap<>(message);
//                copy.put("deviceId",device.getDeviceId());
//                copy.putIfAbsent("messageId",IDGenerator.SNOW_FLAKE_STRING.generate());
//                //复制为新的消息,防止冲突
//                DeviceMessage copiedMessage = MessageType.convertMessage(copy).map(DeviceMessage.class :: cast).orElseThrow(() -> new UnsupportedOperationException("不支持的消息格式"));
//                return device.messageSender().send(copiedMessage).onErrorResume(Exception.class,error -> {
//                    if(copiedMessage instanceof RepayableDeviceMessage){
//                        return Mono.just(((RepayableDeviceMessage)copiedMessage).newReply().error(error));
//                    }
//                    return Mono.error(error);
//                });
//            },Integer.MAX_VALUE);
//        });
//    }

//    //设备功能调用
//    @PostMapping("/{deviceId:.+}/function/{functionId}")
//    @SneakyThrows
//    @QueryAction
//    @Operation(summary = "发送调用设备功能指令到设备", description = "请求示例: {\"参数\":\"值\"}")
//    public Flux<?> invokedFunction(
//        @PathVariable String deviceId,@PathVariable String functionId,@RequestBody Mono<Map<String,Object>> properties
//    ){
//
//        return properties.flatMapMany(props -> service.invokeFunction(deviceId,functionId,props));
//    }


}
