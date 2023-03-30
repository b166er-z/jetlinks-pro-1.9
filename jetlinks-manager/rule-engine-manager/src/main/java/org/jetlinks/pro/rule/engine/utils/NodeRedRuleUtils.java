package org.jetlinks.pro.rule.engine.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.enums.RuleInstanceState;
import org.jetlinks.pro.rule.engine.service.RuleInstanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class NodeRedRuleUtils {

    public static RuleInstanceEntity createFullEntity(RuleInstanceEntity instance) {
        if (instance == null) {
            instance = new RuleInstanceEntity();
        }
        instance.setCreateTime(System.currentTimeMillis());
        instance.setModelId(instance.getId());
        instance.setModelVersion(1);
        instance.setModelType("node-red");
        instance.setState(RuleInstanceState.stopped);
        instance.tryValidate(CreateGroup.class);
        JSONObject model;
        JSONArray flows;
        JSONObject tab = null;
        if (StringUtils.hasText(instance.getModelMeta())) {
            model = JSON.parseObject(instance.getModelMeta());
            flows = model.getJSONArray("flows");
            for (Object flow : flows) {
                JSONObject node = ((JSONObject) flow);
                if ("tab".equals(node.getString("type"))) {
                    tab = node;
                } else {
                    node.put("z", instance.getId());
                }
            }
            if (tab == null) {
                tab = new JSONObject();
                flows.add(0, tab);
            }
        } else {
            model = new JSONObject();
            flows = new JSONArray();
            tab = new JSONObject();
            flows.add(tab);
            model.put("flows", flows);
            model.put("rev", IDGenerator.MD5.generate());
        }
        tab.put("type", "tab");
        tab.put("id", instance.getId());
        tab.put("label", instance.getName());
        tab.put("disabled", false);
        tab.put("info", instance.getDescription());
        instance.setModelMeta(model.toJSONString());
        return instance;
    }

    public static Mono<String> getFlowsInfo(RuleInstanceService instanceService, String flowId){
         return instanceService
            .createQuery()
            .where(RuleInstanceEntity::getId, flowId)
            .and(RuleInstanceEntity::getModelType, "node-red")
            .fetchOne()
            .map(rule -> {
                String json = rule.getModelMeta();
                JSONObject object = JSON.parseObject(json);
                JSONArray array = object.getJSONArray("flows");
                array.stream()
                     .map(JSONObject.class::cast)
                     .filter(node -> "tab".equals(node.getString("type")))
                     .findFirst()
                     .ifPresent(node -> {
                         node.put("label", rule.getName());
                         node.put("info", rule.getDescription());
                         node.put("disabled", rule.getState() == RuleInstanceState.disable);
                     });

                return object.toJSONString();
            });
    }

}
