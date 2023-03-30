package org.jetlinks.pro.rule.engine.model.nodes.network;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HttpRequestNodeConverter implements NodeConverter {


    @Override
    public String getNodeType() {
        return "http request";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();

        model.setExecutor("http-request");

        Map<String, Object> config = new HashMap<>(nodeJson);

        if("auto".equals(nodeJson.getString("method"))) {
            config.remove("method");
        }
        config.remove("x");
        config.remove("y");
        config.remove("z");
        config.remove("wires");

        model.setConfiguration(config);

        return model;
    }
}
