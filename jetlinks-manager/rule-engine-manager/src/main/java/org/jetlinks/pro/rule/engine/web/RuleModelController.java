package org.jetlinks.pro.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.pro.rule.engine.entity.RuleModelEntity;
import org.jetlinks.pro.rule.engine.service.RuleModelService;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.worker.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("rule-engine/model")
@Resource(id = "rule-model", name = "规则引擎-模型")
@TenantAssets(type = "ruleModel")
@Tag(name = "规则模型(已弃用)")
@Deprecated
public class RuleModelController implements TenantAccessCrudController<RuleModelEntity, String> {

    @Autowired
    private RuleModelService ruleModelService;

    @Autowired
    private RuleEngine engine;

    @Override
    public ReactiveCrudService<RuleModelEntity, String> getService() {
        return ruleModelService;
    }

    @PostMapping("/{id}/_deploy")
    @ResourceAction(id = "deploy", name = "发布")
    @Operation(summary = "发布为规则实例")
    public Mono<Boolean> deploy(@PathVariable String id) {
        return ruleModelService.deploy(id);
    }

    //获取全部支持的执行器
    @GetMapping("/executors")
    @QueryAction
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "获取支持的执行器")
    public Flux<String> getAllSupportExecutors() {
        return engine.getWorkers()
            .flatMap(Worker::getSupportExecutors)
            .flatMapIterable(Function.identity())
            .distinct()
            ;
    }

    //获取全部支持的执行器
    @GetMapping("/workers")
    @QueryAction
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "获取Worker")
    public Flux<WorkerInfo> getWorkers() {
        return engine.getWorkers().flatMap(WorkerInfo::of);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class WorkerInfo {
        private String id;

        private String name;

        private List<String> supportExecutors;

        public static Mono<WorkerInfo> of(Worker worker) {
            return worker.getSupportExecutors()
                .map(executors -> {
                    return new WorkerInfo(worker.getId(), worker.getName(), executors);
                });
        }
    }
}
