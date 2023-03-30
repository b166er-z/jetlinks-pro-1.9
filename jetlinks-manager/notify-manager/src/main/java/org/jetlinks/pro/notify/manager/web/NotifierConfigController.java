package org.jetlinks.pro.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.pro.notify.NotifierProvider;
import org.jetlinks.pro.notify.NotifyType;
import org.jetlinks.pro.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.pro.notify.manager.service.NotifyConfigService;
import org.jetlinks.pro.notify.manager.web.response.NotifyTypeInfo;
import org.jetlinks.pro.notify.manager.web.response.ProviderInfo;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifier/config")
@Resource(id = "notifier", name = "通知管理")
@TenantAssets(type = "notifyConfig")
@Tag(name = "消息通知配置")
public class NotifierConfigController implements TenantAccessCrudController<NotifyConfigEntity, String> {

    private final NotifyConfigService notifyConfigService;

    private final List<NotifierProvider> providers;

    public NotifierConfigController(NotifyConfigService notifyConfigService,
                                    List<NotifierProvider> providers) {
        this.notifyConfigService = notifyConfigService;
        this.providers = providers;
    }


    @Override
    public NotifyConfigService getService() {
        return notifyConfigService;
    }

    @GetMapping("/{type}/{provider}/metadata")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取指定类型和服务商所需配置定义")
    public Mono<ConfigMetadata> getAllTypes(@PathVariable @Parameter(description = "通知类型ID") String type,
                                            @PathVariable @Parameter(description = "服务商ID") String provider) {
        return Flux.fromIterable(providers)
            .filter(prov -> prov.getType().getId().equalsIgnoreCase(type) && prov.getProvider().getId().equalsIgnoreCase(provider))
            .flatMap(prov -> Mono.justOrEmpty(prov.getNotifierConfigMetadata()))
            .next();
    }


    @GetMapping("/types")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取平台支持的通知类型")
    public Flux<NotifyTypeInfo> getAllTypes() {
        return Flux.fromIterable(providers)
            .collect(Collectors.groupingBy(NotifierProvider::getType))
            .flatMapIterable(Map::entrySet)
            .map(en -> {
                NotifyTypeInfo typeInfo = new NotifyTypeInfo();
                typeInfo.setId(en.getKey().getId());
                typeInfo.setName(en.getKey().getName());
                typeInfo.setProviderInfos(en.getValue().stream().map(ProviderInfo::of).collect(Collectors.toList()));
                return typeInfo;
            });
    }

    /**
     * 根据类型获取服务商信息
     *
     * @param type 类型标识 {@link NotifyType#getId()}
     * @return 服务商信息
     */
    @GetMapping("/type/{type}/providers")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取支持的服务商")
    public Flux<ProviderInfo> getTypeProviders(@PathVariable
                                               @Parameter(description = "通知类型ID") String type) {
        return Flux
            .fromIterable(providers)
            .filter(provider -> provider.getType().getId().equals(type))
            .map(ProviderInfo::of);
    }

}
