package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveTreeServiceQueryController;
import org.jetlinks.pro.device.entity.DeviceGroupEntity;
import org.jetlinks.pro.device.entity.DeviceGroupInfo;
import org.jetlinks.pro.device.service.DeviceGroupService;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.device.tenant.DeviceGroupAsset;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@RestController
@Resource(id = "device-group", name = "设备分组")
@RequestMapping("/device/group")
@DeviceGroupAsset
@Tag(name = "设备分组管理")
public class DeviceGroupController implements
    TenantAccessCrudController<DeviceGroupEntity, String>,
    ReactiveTreeServiceQueryController<DeviceGroupEntity, String> {

    private final DeviceGroupService deviceGroupService;

    public DeviceGroupController(DeviceGroupService deviceGroupService) {
        this.deviceGroupService = deviceGroupService;
    }

    @Override
    public DeviceGroupService getService() {
        return deviceGroupService;
    }

    @GetMapping("/_query/_detail")
    @QueryAction
    @QueryOperation(summary = "使用GET方式查询设备分组详情")
    public Mono<PagerResult<DeviceGroupInfo>> queryDetail(@Parameter(hidden = true) QueryParamEntity query) {
        return deviceGroupService.queryGroupInfo(query);
    }

    @PostMapping("/_query/_detail")
    @QueryAction
    @Operation(summary = "使用POST方式查询设备分组详情")
    public Mono<PagerResult<DeviceGroupInfo>> queryDetail(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(deviceGroupService::queryGroupInfo);
    }

    @PostMapping("/{groupId}/_bind")
    @SaveAction
    @Operation(summary = "绑定设备到分组")
    public Mono<DeviceGroupInfo> bind(@PathVariable @Parameter(description = "分组ID") String groupId,
                                      @RequestBody @Parameter(description = "设备ID集合") Mono<List<String>> deviceId) {
        return TenantMember
            .assertPermission(deviceId.flatMapIterable(Function.identity()), DeviceAssetType.device, Function.identity())
            .collectList()
            .flatMap(list -> deviceGroupService.bind(groupId, list))
            .then(deviceGroupService.findGroupInfo(groupId));
    }

    @PostMapping("/{groupId}/_unbind")
    @SaveAction
    @Operation(summary = "解绑设备")
    public Mono<DeviceGroupInfo> unbind(@PathVariable @Parameter(description = "分组ID") String groupId,
                                        @RequestBody @Parameter(description = "设备ID集合") Mono<List<String>> deviceId) {
        return TenantMember
            .assertPermission(deviceId.flatMapIterable(Function.identity()), DeviceAssetType.device, Function.identity())
            .collectList()
            .flatMap(list -> deviceGroupService.unbind(groupId, list))
            .then(deviceGroupService.findGroupInfo(groupId));
    }

    @PostMapping("/{groupId}/_unbind/all")
    @SaveAction
    @Operation(summary = "解绑分组下所有设备")
    public Mono<DeviceGroupInfo> unbindAll(@PathVariable @Parameter(description = "分组ID") String groupId) {
        return deviceGroupService
            .unbind(groupId)
            .then(deviceGroupService.findGroupInfo(groupId));
    }

}
