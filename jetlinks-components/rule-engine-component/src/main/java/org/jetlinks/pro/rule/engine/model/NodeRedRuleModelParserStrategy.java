package org.jetlinks.pro.rule.engine.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.scheduler.SchedulingRule;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Node-Red 规则模型解析器策略，用于将Node-Red编辑器对规则模型解析为{@link RuleModel}
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
public class NodeRedRuleModelParserStrategy implements RuleModelParserStrategy, ApplicationContextAware {

    Map<String, NodeConverter> converts = new ConcurrentHashMap<>();

    @Override
    public String getFormat() {
        return "node-red";
    }

    @Override
    public RuleModel parse(String modelDefineString) {

        JSONObject json = JSON.parseObject(modelDefineString);

        JSONArray flows = json.getJSONArray("flows");

        Map<String, Tuple3<JSONObject, RuleNodeModel, NodeConverter>> allModel = new HashMap<>();
        RuleModel model = new RuleModel();

        Map<String, JSONObject> nodeMapping = flows
            .stream()
            .map(JSONObject.class::cast)
            .collect(Collectors.toMap(node -> node.getString("id"), Function.identity()));

        for (JSONObject node : nodeMapping.values()) {
            String id = node.getString("id");
            String type = node.getString("type");
            //调度规则
            if ("schedule-rule".equals(type)) {
                continue;
            }
            //流程定义
            if ("tab".equals(type)) {
                model.setId(id);
                model.setName(node.getString("label"));
                continue;
            }
            if (Boolean.TRUE.equals(node.getBoolean("d"))) {
                continue;
            }
            NodeConverter converter = Optional
                .ofNullable(converts.get(type))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的节点类型:" + type));

            RuleNodeModel nodeModel = converter.convert(node);
            if (nodeModel == null) {
                continue;
            }

            if (node.containsKey("scheduleRule")
                && nodeModel.getSchedulingRule() == null) {
                JSONObject scheduleNode = nodeMapping.get(node.getString("scheduleRule"));
                if (scheduleNode != null && scheduleNode.containsKey("scheduleType")) {
                    SchedulingRule rule = new SchedulingRule();
                    rule.setType(scheduleNode.getString("scheduleType"));
                    rule.setConfiguration(scheduleNode.getJSONObject("configuration"));
                    nodeModel.setSchedulingRule(rule);
                }
            }
            if (StringUtils.isEmpty(nodeModel.getExecutor())) {
                nodeModel.setExecutor(type);
            }
            nodeModel.setId(id);
            Optional.ofNullable(node.getString("name"))
                    .ifPresent(nodeModel::setName);
            allModel.put(id, Tuples.of(node, nodeModel, converter));

        }

        for (Map.Entry<String, Tuple3<JSONObject, RuleNodeModel, NodeConverter>> parsed : allModel.entrySet()) {
            String id = parsed.getKey();
            JSONObject config = parsed.getValue().getT1();
            RuleNodeModel nodeModel = parsed.getValue().getT2();
            NodeConverter converter = parsed.getValue().getT3();
            List<String> outputs = getLinks(config.getJSONArray("wires"));

            //事件监听器
            if (converter.isEventListener()) {
                List<String> scopes = getLinks(config.getJSONArray("scope"));
                for (String scope : scopes) {
                    for (String output : outputs) {
                        if (allModel.get(scope) == null) {
                            throw new UnsupportedOperationException("无法获取监听节点:" + scope);
                        }
                        RuleNodeModel listen = allModel.get(scope).getT2();
                        RuleLink link = new RuleLink();
                        link.setId(scope + ":" + output);
                        link.setType(converter.getEventType());//事件类型
                        link.setSource(listen);

                        link.setTarget(allModel.get(output).getT2());

                        listen.getEvents().add(link);
                    }
                }

            } else {
                model.getNodes().add(nodeModel);
                for (String output : outputs) {
                    if (!allModel.containsKey(output)) {
                        continue;
                    }
                    RuleNodeModel outputModel = allModel.get(output).getT2();

                    RuleLink link = new RuleLink();
                    link.setId(id + ":" + output);
                    link.setSource(nodeModel);
                    link.setTarget(outputModel);

                    nodeModel.getOutputs().add(link);
                    outputModel.getInputs().add(link);
                }
            }
        }
        return model;
    }

    private List<String> getLinks(JSONArray arr) {
        if (CollectionUtils.isEmpty(arr)) {
            return Collections.emptyList();
        }
        return arr
            .stream()
            .flatMap(obj -> {
                if (obj instanceof Collection) {
                    return ((Collection<?>) obj).stream();
                }
                return Stream.of(obj);
            })
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    public void addNodeConverter(NodeConverter converter) {
        converts.put(converter.getNodeType(), converter);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        applicationContext
            .getBeansOfType(NodeConverter.class)
            .values()
            .forEach(this::addNodeConverter);
    }
}
