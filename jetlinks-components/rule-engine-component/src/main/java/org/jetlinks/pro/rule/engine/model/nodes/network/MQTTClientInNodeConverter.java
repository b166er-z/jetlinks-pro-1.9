package org.jetlinks.pro.rule.engine.model.nodes.network;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MQTTClientInNodeConverter implements NodeConverter {


    @Override
    public String getNodeType() {
        return "mqtt in";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();

        model.setExecutor("mqtt-client");

        Map<String, Object> config = new HashMap<>();
        config.put("clientId", nodeJson.getString("clientId"));
        config.put("topics", nodeJson.getString("topic"));
        config.put("topicVariables", nodeJson.getString("topicVariables"));
        config.put("clientType", "consumer");
        model.setConfiguration(config);

        return model;
    }
}
