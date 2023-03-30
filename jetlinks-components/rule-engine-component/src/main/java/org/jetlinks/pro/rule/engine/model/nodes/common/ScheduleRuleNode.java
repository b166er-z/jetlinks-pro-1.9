package org.jetlinks.pro.rule.engine.model.nodes.common;

import org.jetlinks.pro.rule.engine.editor.AbstractEditorNode;
import org.jetlinks.pro.rule.engine.editor.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class ScheduleRuleNode extends AbstractEditorNode {

    public static final String ID = "schedule-rule";

    public ScheduleRuleNode() {
        setResource(ResourceType.editor, "rule-engine/editor/common/1-schedule-rule.html");
        setResource(ResourceType.helper, "rule-engine/i18n/zh-CN/common/1-schedule-rule.html");
    }

    @Override
    public String getId() {
        return ScheduleRuleNode.ID;
    }

    @Override
    public String getName() {
        return "调度规则";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
