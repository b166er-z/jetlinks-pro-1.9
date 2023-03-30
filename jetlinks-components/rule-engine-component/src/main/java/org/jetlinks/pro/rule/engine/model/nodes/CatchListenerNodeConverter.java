package org.jetlinks.pro.rule.engine.model.nodes;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

@Component
@EditorResource(
    id = "catch",
    name = "捕获错误",
    editor = "rule-engine/editor/common/25-catch.html",
    helper = "rule-engine/i18n/zh-CN/common/25-catch.html",
    order = 50
)
public class CatchListenerNodeConverter implements NodeConverter {
    @Override
    public String getNodeType() {
        return "catch";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {

        RuleNodeModel model = new RuleNodeModel();
        model.setExecutor("direct");

        return model;
    }

    @Override
    public String getEventType() {
        return RuleConstants.Event.error;
    }

    @Override
    public boolean isEventListener() {
        return true;
    }
}
