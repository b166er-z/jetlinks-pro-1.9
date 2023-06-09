package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.enums.DeviceType;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.device.web.response.GatewayDeviceInfo;
import org.jetlinks.pro.tenant.TenantMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 网关设备接口
 *
 * @author zhouhao
 * @since 1.0
 */
@RestController
@RequestMapping("/device/gateway")
@Resource(id = "device-gateway", name = "网关设备管理")
@Authorize
@DeviceAsset
@Tag(name = "网关设备管理")
public class GatewayDeviceController {

    @Autowired
    private LocalDeviceInstanceService instanceService;

    @Autowired
    private LocalDeviceProductService productService;

    @Autowired
    private DeviceRegistry registry;

    @SuppressWarnings("all")
    private Mono<List<String>> getGatewayProductList() {
        return productService
            .createQuery()
            .select(DeviceProductEntity::getId)
            .where(DeviceProductEntity::getDeviceType, DeviceType.gateway)
            .fetch()
            .map(DeviceProductEntity::getId)
            .collectList()
            .filter(CollectionUtils::isNotEmpty);
    }

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询网关设备详情")
    public Mono<PagerResult<GatewayDeviceInfo>> queryGatewayDevice(@Parameter(hidden = true) QueryParamEntity param) {
        return getGatewayProductList()
            .flatMap(productIdList ->
                param.toNestQuery(query -> query.in(DeviceInstanceEntity::getProductId, productIdList))
                    .execute(instanceService::queryPager)
                    .filter(r -> r.getTotal() > 0)
                    .flatMap(result -> {
                        Map<String, DeviceInstanceEntity> mapping =
                            result.getData()
                                .stream()
                                .collect(Collectors.toMap(DeviceInstanceEntity::getId, Function.identity()));

                        //查询所有子设备并按父设备ID分组
                        return instanceService.createQuery()
                            .where()
                            .in(DeviceInstanceEntity::getParentId, mapping.keySet())
                            .fetch()
                            .groupBy(DeviceInstanceEntity::getParentId,Integer.MAX_VALUE)
                            .flatMap(group -> {
                                String parentId = group.key();
                                return group
                                    .collectList()
                                    //将父设备和分组的子设备合并在一起
                                    .map(children -> GatewayDeviceInfo.of(mapping.get(parentId), children));
                            })
                            .collectMap(GatewayDeviceInfo::getId)//收集所有有子设备的网关设备信息
                            .defaultIfEmpty(Collections.emptyMap())
                            .flatMapMany(map -> Flux.fromIterable(mapping.values())
                                .flatMap(ins -> Mono.justOrEmpty(map.get(ins.getId()))
                                    //处理没有子设备的网关信息
                                    .switchIfEmpty(Mono.fromSupplier(() -> GatewayDeviceInfo.of(ins, Collections.emptyList())))))
                            .collectList()
                            .map(list -> PagerResult.of(result.getTotal(), list, param));
                    }))
            .defaultIfEmpty(PagerResult.empty());
    }

    @GetMapping("/{id}")
    @QueryAction
    @DeviceAsset
    @QueryOperation(summary = "获取单个网关设备详情")
    public Mono<GatewayDeviceInfo> getGatewayInfo(@PathVariable String id) {
        return Mono.zip(
            instanceService.findById(id),
            instanceService.createQuery()
                .where()
                .is(DeviceInstanceEntity::getParentId, id)
                .fetch()
                .collectList()
                .defaultIfEmpty(Collections.emptyList()),
            GatewayDeviceInfo::of);
    }


    @PostMapping("/{gatewayId}/bind/{deviceId}")
    @SaveAction
    @DeviceAsset(ignore = true)
    @QueryOperation(summary = "绑定单个子设备到网关设备")
    public Mono<GatewayDeviceInfo> bindDevice(@PathVariable @Parameter(description = "网关设备ID") String gatewayId,
                                              @PathVariable @Parameter(description = "子设备ID") String deviceId) {
        return TenantMember
            .assertPermission(Flux.just(gatewayId, deviceId), DeviceAssetType.device, Function.identity())
            .then(
                instanceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getParentId, gatewayId)
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .then(registry
                        .getDevice(deviceId)
                        .flatMap(operator -> operator.setConfig(DeviceConfigKey.parentGatewayId, gatewayId)))
                    .then(getGatewayInfo(gatewayId))
            );
    }

    @PostMapping("/{gatewayId}/bind")
    @SaveAction
    @DeviceAsset(ignore = true)
    @QueryOperation(summary = "绑定多个子设备到网关设备")
    public Mono<GatewayDeviceInfo> bindDevice(@PathVariable @Parameter(description = "网关设备ID") String gatewayId,
                                              @RequestBody @Parameter(description = "子设备ID集合") Mono<List<String>> deviceId) {


        return Mono.zip(
            //是否有权限操作这个网关
            TenantMember.assertPermission(
                Mono.just(gatewayId), DeviceAssetType.device, Function.identity()),
            //是否有权限绑定这些设备
            TenantMember.assertPermission(
                deviceId.flatMapIterable(Function.identity()), DeviceAssetType.device, Function.identity()).collectList())
            .flatMap(
                tp2 -> Mono.just(tp2.getT2())
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(deviceIdList -> instanceService
                        .createUpdate()
                        .set(DeviceInstanceEntity::getParentId, tp2.getT1())
                        .where()
                        .in(DeviceInstanceEntity::getId, deviceIdList)
                        .execute()
                        .then(Flux
                            .fromIterable(deviceIdList)
                            .flatMap(id -> registry
                                .getDevice(id)
                                .flatMap(operator -> operator.setConfig(DeviceConfigKey.parentGatewayId, gatewayId))).then()
                        )))
            .then(getGatewayInfo(gatewayId));


    }

    @PostMapping("/{gatewayId}/unbind/{deviceId}")
    @SaveAction
    @QueryOperation(summary = "从网关设备中解绑子设备")
    public Mono<GatewayDeviceInfo> unBindDevice(@PathVariable @Parameter(description = "网关设备ID") String gatewayId,
                                                @PathVariable @Parameter(description = "自设备ID") String deviceId) {
        return TenantMember
            .assertPermission(Flux.just(gatewayId, deviceId), DeviceAssetType.device, Function.identity())
            .then(instanceService
                .createUpdate()
                .setNull(DeviceInstanceEntity::getParentId)
                .where(DeviceInstanceEntity::getId, deviceId)
                .and(DeviceInstanceEntity::getParentId, gatewayId)
                .execute()
                .filter(i -> i > 0)
                .flatMap(i -> registry
                    .getDevice(deviceId)
                    .flatMap(operator -> operator.removeConfig(DeviceConfigKey.parentGatewayId.getKey())))
                .then(getGatewayInfo(gatewayId)));
    }

}
