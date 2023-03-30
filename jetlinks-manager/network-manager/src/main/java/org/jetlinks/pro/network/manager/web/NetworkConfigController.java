package org.jetlinks.pro.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.NetworkProvider;
import org.jetlinks.pro.network.ServerNetwork;
import org.jetlinks.pro.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.pro.network.manager.enums.NetworkConfigState;
import org.jetlinks.pro.network.manager.service.NetworkConfigService;
import org.jetlinks.pro.network.manager.tenant.NetworkAssetType;
import org.jetlinks.pro.network.manager.web.response.NetworkConfigInfoResponse;
import org.jetlinks.pro.network.manager.web.response.NetworkTypeInfo;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0
 **/
@RestController
@RequestMapping("/network/config")
@Resource(id = "network-config", name = "网络组件配置")
@Authorize
@TenantAssets(type = "network")
@Tag(name = "网络组件管理")
public class NetworkConfigController implements TenantAccessCrudController<NetworkConfigEntity, String> {

    private final NetworkConfigService configService;

    private final NetworkManager networkManager;

    public NetworkConfigController(NetworkConfigService configService, NetworkManager networkManager) {
        this.configService = configService;
        this.networkManager = networkManager;
    }

    @Override
    public NetworkConfigService getService() {
        return configService;
    }


    @GetMapping("/{networkType}/_detail")
    @QueryAction
    @Operation(summary = "获取指定类型下所有网络组件信息")
    @TenantAssets(ignore = true)
    public Flux<NetworkConfigInfoResponse> getNetworkInfo(@PathVariable
                                                  @Parameter(description = "网络组件类型") String networkType) {

        return TenantMember
            .injectQueryParam(QueryParamEntity.of(), NetworkAssetType.network, "id")
            .flatMapMany(param -> configService
                .createQuery()
                .setParam(param)
                .where(NetworkConfigEntity::getType, networkType)
                .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
                .fetch())
            .flatMap(config -> {
                Mono<NetworkConfigInfoResponse> def = Mono.just(NetworkConfigInfoResponse.of(config.getId(), config.getName(), ""));
                if (config.getState() == NetworkConfigState.enabled) {
                    return networkManager
                        .getNetwork(DefaultNetworkType.valueOf(networkType.toUpperCase()), config.getId())
                        .filter(ServerNetwork.class::isInstance)
                        .cast(ServerNetwork.class)
                        .map(server -> NetworkConfigInfoResponse.of(config.getId(), config.getName(), String.valueOf(server.getBindAddress())))
                        .onErrorResume(err -> def)
                        .switchIfEmpty(def);
                }
                return def;
            });
    }


    @GetMapping("/supports")
    @Operation(summary = "获取支持的网络组件类型")
    @TenantAssets(ignore = true)
    public Flux<NetworkTypeInfo> getSupports() {
        return Flux.fromIterable(networkManager.getProviders())
                   .map(NetworkProvider::getType)
                   .map(NetworkTypeInfo::of);
    }

    @PostMapping("/{id}/_start")
    @SaveAction
    @Operation(summary = "启动网络组件")
    public Mono<Void> start(@PathVariable
                            @Parameter(description = "网络组件ID") String id) {
        return configService.start(id);
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止网络组件")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网络组件ID") String id) {
        return configService.shutdown(id);
    }

}
