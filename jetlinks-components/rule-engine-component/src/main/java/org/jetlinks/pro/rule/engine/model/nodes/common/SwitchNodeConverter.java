package org.jetlinks.pro.rule.engine.model.nodes.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SwitchNodeConverter implements NodeConverter {
    @Override
    public String getNodeType() {
        return "switch";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();
        model.setExecutor("switch");

        Map<String, Object> config = new HashMap<>();

        JSONArray rules = nodeJson.getJSONArray("rules");

        JSONArray wires = nodeJson.getJSONArray("wires");

        config.put("property", nodeJson.getString("property"));
        config.put("propertyType", nodeJson.getString("propertyType"));
        config.put("checkAll", nodeJson.getString("checkall"));

        List<Map<String, Object>> conditions = new ArrayList<>();

        for (int i = 0; i < rules.size(); i++) {
            JSONObject conf = rules.getJSONObject(i);
            JSONArray wire = wires.getJSONArray(i);
            Map<String, Object> condition = new HashMap<>();
            condition.put("type", conf.getString("t"));
            condition.put("value", conf.getString("v"));
            condition.put("valueType", conf.getString("vt"));
            condition.put("outputs", wire);
            conditions.add(condition);
        }

        config.put("conditions", conditions);

        nodeJson.remove("wires");

        model.setConfiguration(config);

        return model;
    }
}
