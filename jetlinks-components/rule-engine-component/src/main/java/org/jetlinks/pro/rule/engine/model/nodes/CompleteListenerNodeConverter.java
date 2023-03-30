package org.jetlinks.pro.rule.engine.model.nodes;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@EditorResource(
    id = "complete",
    name = "监听完成",
    editor = "rule-engine/editor/common/24-complete.html",
    helper = "rule-engine/i18n/zh-CN/common/24-complete.html",
    order = 40
)
public class CompleteListenerNodeConverter implements NodeConverter {
    @Override
    public String getNodeType() {
        return "complete";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {

        RuleNodeModel model = new RuleNodeModel();
        model.setExecutor("direct");

        return model;
    }

    @Override
    public String getEventType() {
        return RuleConstants.Event.complete;
    }

    @Override
    public boolean isEventListener() {
        return true;
    }
}
