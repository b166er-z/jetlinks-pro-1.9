package org.jetlinks.pro.rule.engine.model.nodes.network;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HttpClientResponseNodeConverter implements NodeConverter {


    @Override
    public String getNodeType() {
        return "http response";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();

        model.setExecutor("http-response");

        Map<String, Object> config = new HashMap<>();
        config.put("status", nodeJson.getInteger("statusCode"));
        config.put("headers", nodeJson.getJSONObject("headers"));
        model.setConfiguration(config);

        return model;
    }
}
