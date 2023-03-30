package org.jetlinks.pro.rule.engine.web.editor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.pro.rule.engine.editor.EditorNode;
import org.jetlinks.pro.rule.engine.editor.EditorNodeManager;
import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.enums.RuleInstanceState;
import org.jetlinks.pro.rule.engine.service.RuleInstanceService;
import org.jetlinks.pro.rule.engine.tenant.RuleEngineAssetType;
import org.jetlinks.pro.rule.engine.utils.NodeRedRuleUtils;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.rule.engine.api.scope.FlowScope;
import org.jetlinks.rule.engine.api.scope.NodeScope;
import org.jetlinks.rule.engine.api.scope.PersistenceScope;
import org.jetlinks.rule.engine.cluster.scope.ClusterGlobalScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rule-editor")
@AllArgsConstructor
@org.hswebframework.web.authorization.annotation.Resource(id = "rule-instance", name = "规则引擎-实例")
@Authorize
@Hidden
public class RuleEngineEditorController {

    private final RuleInstanceService instanceService;

    private final ClusterManager clusterManager;

    private final EditorNodeManager nodeManager;

    protected Mono<ResponseEntity<Map<String, Object>>> createContextResponse(PersistenceScope scope) {
        return Mono
            .zip(
                scope.all(),
                scope.counter().get()
            )
            .map(tp2 -> {
                Map<String, Object> value = new HashMap<>();
                value.put("context", tp2.getT1());
                value.put("counter", Collections.singletonMap("_counter", tp2.getT2()));
                return ResponseEntity.ok(value);
            });
    }

    protected Mono<ResponseEntity<Map<String, Object>>> createContextResponse(PersistenceScope scope, String key) {
        if ("_counter".equals(key)) {
            return scope
                .counter()
                .get()
                .map(val -> ResponseEntity.ok(Collections.singletonMap("msg", val)))
                ;
        }
        return scope
            .get(key)
            .map(val -> ResponseEntity.ok(Collections.singletonMap("msg", val)));
    }

    @GetMapping("/context/flow/{flowId}/node/{nodeId}/{key}")
    @SneakyThrows
    public Mono<ResponseEntity<Map<String, Object>>> getFlowContextValue(@PathVariable String flowId,
                                                                         @PathVariable String nodeId,
                                                                         @PathVariable String key) {

        return createContextResponse(new ClusterGlobalScope(clusterManager)
                                         .flow(flowId)
                                         .node(nodeId), key);
    }

    @GetMapping("/context/flow/{flowId}/{key}")
    @SneakyThrows
    public Mono<ResponseEntity<Map<String, Object>>> getFlowContextValue(@PathVariable String flowId,
                                                                         @PathVariable String key) {

        return createContextResponse(new ClusterGlobalScope(clusterManager)
                                         .flow(flowId), key);
    }


    @GetMapping("/context/global/{key}")
    @SneakyThrows
    public Mono<ResponseEntity<Map<String, Object>>> getGlobalContext(@PathVariable String key) {
        return createContextResponse(new ClusterGlobalScope(clusterManager), key);
    }


    @GetMapping("/context/flow/{flowId}/node/{nodeId}")
    @SneakyThrows
    public Mono<ResponseEntity<Map<String, Object>>> getNodeContext(@PathVariable String flowId,
                                                                    @PathVariable String nodeId) {

        return createContextResponse(new ClusterGlobalScope(clusterManager).flow(flowId).node(nodeId));
    }

    @GetMapping("/context/flow/{flowId}")
    @SneakyThrows
    public Mono<ResponseEntity<Map<String, Object>>> getFlowContext(@PathVariable String flowId) {
        return createContextResponse(new ClusterGlobalScope(clusterManager).flow(flowId));
    }

    @GetMapping("/context/global")
    @SneakyThrows
    public Mono<ResponseEntity<Map<String, Object>>> getGlobalContext() {
        return createContextResponse(new ClusterGlobalScope(clusterManager));
    }


    @GetMapping("/icons")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getIcons() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/icons.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping("/nodes")
    @SneakyThrows
    @Authorize(ignore = true)
    public Mono<ResponseEntity<String>> getNodes() {
        return nodeManager
            .getNodes()
            .sort()
            .map(this::convertNode)
            .collectList()
            .map(JSON::toJSONString)
            .map(ResponseEntity::ok);
//
//        try (InputStream stream = new ClassPathResource("static/rule-editor/nodes.json").getInputStream()) {
//            return Mono.just(
//                ResponseEntity.ok(
//                    StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
//                )
//            );
//        }
    }

    protected JSONObject convertNode(EditorNode node) {
        JSONObject object = new JSONObject();
        object.put("id", node.getId());
        object.put("name", node.getName());
        object.put("types", node.getTypes());
        object.put("enabled", node.isEnabled());
        object.put("module", "node-red");
        return object;
    }

    @GetMapping("/flows")
    @SneakyThrows
    @TenantAssets(type = "ruleInstance")
    @QueryAction
    public Mono<ResponseEntity<String>> getFlows(@RequestParam String id) {
        return NodeRedRuleUtils
            .getFlowsInfo(instanceService, id)
            .map(ResponseEntity::ok);
    }

    @PostMapping("/flows/_create")
    @TenantAssets(type = "ruleInstance", assetObjectIndex = 0, autoBind = true, allowAssetNotExist = true)
    @SaveAction
    public Mono<Void> createFlow(@RequestBody Mono<RuleInstanceEntity> body) {
        return body.flatMap(instance ->
                                instanceService.insert(Mono.just(NodeRedRuleUtils.createFullEntity(instance))))
                   .then();
    }

    @PostMapping("/flows")
    @SneakyThrows
    @TenantAssets(ignore = true)
    @SaveAction
    public Mono<Void> saveFlow(@RequestBody Mono<String> model,
                               @RequestHeader(value = "Node-RED-Deployment-Type", required = false) String type) {

        return model.flatMap(modelDefineString -> {
            JSONObject json = JSON.parseObject(modelDefineString);
            JSONArray flows = json.getJSONArray("flows");
            return Flux.fromIterable(flows)
                       .map(JSONObject.class::cast)
                       .filter(node -> "tab".equals(node.getString("type")))
                       .singleOrEmpty()
                       .as(data -> TenantMember
                           .assertPermission(data,
                                             RuleEngineAssetType.ruleInstance,
                                             node -> node.getString("id")))
                       .flatMap(node -> instanceService
                           .createUpdate()
                           .set(RuleInstanceEntity::getModelMeta, modelDefineString)
                           .where(RuleInstanceEntity::getId, node.getString("id"))
                           .execute()
                           .then(Mono.defer(() -> "full".equals(type)
                               ? instanceService.stop(node.getString("id"))
                                                .then(instanceService.start(node.getString("id")))
                               : Mono.empty()))
                       );
        });

    }

    @GetMapping("/settings")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getSettings() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/settings.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping("/theme")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getTheme() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/theme.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping("/nodes/messages")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getNodesMessages() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/nodes/messages.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping("/locales/editor")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getLocalesEditor() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/locales/editor.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }

    }

    @GetMapping("/locales/infotips")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getInfoTips() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/locales/infotips.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping("/locales/jsonata")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getJsonata() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/locales/jsonata.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping("/locales/node-red")
    @SneakyThrows
    @Authorize(ignore = true)
    public ResponseEntity<String> getNodeRed() {
        try (InputStream stream = new ClassPathResource("static/rule-editor/locales/node-red.json").getInputStream()) {
            return ResponseEntity.ok(
                StreamUtils.copyToString(stream, StandardCharsets.UTF_8)
            );
        }
    }

    @GetMapping(value = "/nodes", produces = MediaType.TEXT_HTML_VALUE)
    @SneakyThrows
    @Authorize(ignore = true)
    public Mono<ResponseEntity<String>> getNodesHtml() {
        return nodeManager
            .getNodes()
            .sort()
            .flatMapIterable(node -> node.getEditorResources().values())
            .map(resource -> {
                try (InputStream stream = resource.getInputStream()) {
                    return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
                } catch (Throwable ignore) {
                    return "";
                }
            })
            .collect(Collectors.joining(""))
            .map(ResponseEntity::ok)
            ;

//
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//
//        List<Resource> resources = new ArrayList<>();
//        resources.addAll(Arrays.asList(resolver.getResources("classpath:/static/rule-editor/nodes/*/*.html")));
//        resources.addAll(Arrays.asList(resolver.getResources("classpath:/static/rule-editor/locales/zh-CN/*/*.html")));
//
//
//        resources.sort(Comparator.comparing(resource -> {
//            String name = resource.getFilename();
//            if (name != null && name.contains("-")) {
//                String[] arr = name.split("[-]");
//                return Integer.parseInt(arr[0]);
//            }
//            return 0;
//
//        }));
//
//        StringBuilder builder = new StringBuilder();
//
//        for (Resource resource : resources) {
//            try (InputStream stream = resource.getInputStream()) {
//                builder.append(StreamUtils.copyToString(stream, StandardCharsets.UTF_8));
//            }
//        }
//
//        return Mono.just(
//            ResponseEntity.ok(
//                builder.toString()
//            )
//        );
    }

}
