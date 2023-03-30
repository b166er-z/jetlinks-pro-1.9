package org.jetlinks.pro.rule.engine.model.nodes.function;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DeviceMessageSenderNodeConverter implements NodeConverter {


    @Override
    public String getNodeType() {
        return "device-message-sender";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();

        model.setExecutor("device-message-sender");

        Map<String, Object> config = new HashMap<>();
        config.put("from", nodeJson.getString("from"));
        config.put("timeout", nodeJson.getString("timeout"));
        config.put("waitType", nodeJson.getString("waitType"));
        config.put("stateOperator", nodeJson.getString("stateOperator"));
        config.put("selector", nodeJson.getString("selector"));
        config.put("message", JSON.parse(nodeJson.getString("message")));
        model.setConfiguration(config);

        return model;
    }
}
