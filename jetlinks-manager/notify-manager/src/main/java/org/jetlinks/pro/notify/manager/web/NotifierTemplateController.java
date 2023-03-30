package org.jetlinks.pro.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.pro.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.pro.notify.manager.service.NotifyTemplateService;
import org.jetlinks.pro.notify.template.TemplateProvider;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author wangzheng
 * @author zhouhao
 * @since 1.0
 */
@RestController
@RequestMapping("/notifier/template")
@Authorize
@Resource(id = "template", name = "通知模板")
@TenantAssets(type = "notifyTemplate")
@Tag(name = "消息通知模版")
public class NotifierTemplateController implements TenantAccessCrudController<NotifyTemplateEntity, String> {

    private final NotifyTemplateService templateService;

    private final List<TemplateProvider> providers;

    public NotifierTemplateController(NotifyTemplateService templateService, List<TemplateProvider> providers) {
        this.templateService = templateService;
        this.providers = providers;
    }

    @Override
    public ReactiveCrudService<NotifyTemplateEntity, String> getService() {
        return templateService;
    }


    @GetMapping("/{type}/{provider}/config/metadata")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取指定类型和服务商所需模版配置定义")
    public Mono<ConfigMetadata> getAllTypes(@PathVariable @Parameter(description = "通知类型ID") String type,
                                            @PathVariable @Parameter(description = "服务商ID") String provider) {
        return Flux.fromIterable(providers)
            .filter(prov -> prov.getType().getId().equalsIgnoreCase(type) && prov.getProvider().getId().equalsIgnoreCase(provider))
            .flatMap(prov -> Mono.justOrEmpty(prov.getTemplateConfigMetadata()))
            .next();
    }

}
