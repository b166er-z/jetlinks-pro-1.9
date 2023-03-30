package org.jetlinks.pro.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.pro.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.jetlinks.pro.rule.engine.service.DeviceAlarmHistoryService;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;

@RestController
@RequestMapping("/device/alarm/history")
@Resource(id = "device-alarm", name = "设备告警")
@Authorize
@TenantAssets(type = "device", property = "deviceId")
@Tag(name = "设备告警记录")
public class DeviceAlarmHistoryController implements ReactiveServiceQueryController<DeviceAlarmHistoryEntity, String> {

    private final DeviceAlarmHistoryService historyService;

    public DeviceAlarmHistoryController(DeviceAlarmHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public ReactiveCrudService<DeviceAlarmHistoryEntity, String> getService() {
        return historyService;
    }

    @PutMapping("/{id}/_{state}")
    @TenantAssets(ignore = true)
    @Operation(summary = "修改告警记录状态")
    public Mono<Integer> changeState(@PathVariable @Parameter(description = "告警记录ID") String id,
                                     @PathVariable @Parameter(description = "状态") String state,
                                     @RequestBody @Parameter(description = "备注") Mono<String> descriptionMono) {
        return TenantMember
            .assertPermission(historyService.findById(id), "device", DeviceAlarmHistoryEntity::getDeviceId)
            .then(
                descriptionMono
                    .flatMap(description -> historyService
                        .createUpdate()
                        .set(DeviceAlarmHistoryEntity::getUpdateTime, new Date())
                        .set(DeviceAlarmHistoryEntity::getState, state)
                        .set(DeviceAlarmHistoryEntity::getDescription, description)
                        .where(DeviceAlarmHistoryEntity::getId, id)
                        .execute())
            );
    }


}
