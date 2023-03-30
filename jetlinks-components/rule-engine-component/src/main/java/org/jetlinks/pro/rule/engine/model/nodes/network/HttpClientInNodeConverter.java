package org.jetlinks.pro.rule.engine.model.nodes.network;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HttpClientInNodeConverter implements NodeConverter {


    @Override
    public String getNodeType() {
        return "http in";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();

        model.setExecutor("http-listener");

        Map<String, Object> config = new HashMap<>();
        config.put("serverId", nodeJson.getString("serverId"));
        config.put("url", nodeJson.getString("url"));
        config.put("method", nodeJson.getString("method").toUpperCase());
        model.setConfiguration(config);

        return model;
    }
}
