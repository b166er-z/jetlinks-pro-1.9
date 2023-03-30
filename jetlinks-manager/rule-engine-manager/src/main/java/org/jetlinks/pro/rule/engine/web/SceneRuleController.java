package org.jetlinks.pro.rule.engine.web;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.rule.engine.device.SceneRule;
import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.enums.RuleInstanceState;
import org.jetlinks.pro.rule.engine.service.RuleInstanceService;
import org.jetlinks.pro.rule.engine.web.response.SceneRuleInfo;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/rule-engine/scene")
@Resource(id = "rule-scene", name = "场景管理")
@TenantAssets(type = "scene")
@Tag(name = "场景管理")
@AllArgsConstructor
public class SceneRuleController {

    private final RuleInstanceService instanceService;

    private final RuleEngine ruleEngine;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取场景配置")
    @QueryAction
    public Mono<SceneRuleInfo> getScene(@PathVariable String id) {

        return instanceService
            .findById(id)
            .map(SceneRuleInfo::of);
    }


    @PostMapping("/{id}/execute")
    @Operation(summary = "执行场景")
    @ResourceAction(id = "execute", name = "执行场景")
    public Mono<Void> execute(@PathVariable String id, @RequestBody(required = false) Mono<Map<String, Object>> args) {

        return args
            .defaultIfEmpty(Collections.emptyMap())
            .flatMap(data -> ruleEngine.getTasks(id)
                .filter(task -> task.getJob().getNodeId().equals(id))
                .flatMap(task -> task.execute(RuleData.create(data)))
                .then());
    }

    @GetMapping("/_query")
    @QueryOperation(summary = "分页查询场景")
    @QueryAction
    public Mono<PagerResult<SceneRuleInfo>> queryScene(@Parameter(hidden = true) QueryParamEntity query) {

        return query
            .toQuery()
            .where(RuleInstanceEntity::getModelType, SceneRule.modelType)
            .execute(param -> instanceService.queryPager(query, SceneRuleInfo::of));
    }

    @GetMapping("/_query/no-paging")
    @QueryOperation(summary = "查询场景,不返回分页结果")
    @QueryAction
    public Flux<SceneRuleInfo> querySceneNoPaging(@Parameter(hidden = true) QueryParamEntity query) {
        return query.toQuery()
            .where(RuleInstanceEntity::getModelType, SceneRule.modelType)
            .execute(instanceService::query)
            .map(SceneRuleInfo::of);
    }


    @PatchMapping
    @TenantAssets(autoBind = true, assetObjectIndex = 0)
    @Operation(summary = "保存场景")
    @SaveAction
    public Mono<Void> saveScene(@RequestBody Mono<SceneRule> ruleMono) {

        return ruleMono
            .map(SceneRule::toInstance)
            .as(instanceService::save)
            .then();
    }

    @DeleteMapping("/{id}")
    @TenantAssets(autoUnbind = true)
    @Operation(summary = "删除场景")
    @DeleteAction
    public Mono<Void> deleteScene(@PathVariable String id) {

        return instanceService.stop(id)
            .then(instanceService.deleteById(Mono.just(id)))
            .then();
    }

    @PostMapping("/{id}/_start")
    @TenantAssets
    @Operation(summary = "启动场景")
    @SaveAction
    public Mono<Void> startScene(@PathVariable String id) {

        return instanceService.start(id);
    }

    @PostMapping("/{id}/_stop")
    @TenantAssets
    @Operation(summary = "停止场景")
    @SaveAction
    public Mono<Void> stopScene(@PathVariable String id) {

        return instanceService.stop(id);
    }


}
