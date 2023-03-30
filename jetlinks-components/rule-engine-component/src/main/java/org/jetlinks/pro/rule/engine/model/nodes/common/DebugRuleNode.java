package org.jetlinks.pro.rule.engine.model.nodes.common;

import org.jetlinks.pro.rule.engine.editor.AbstractEditorNode;
import org.jetlinks.pro.rule.engine.editor.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class DebugRuleNode extends AbstractEditorNode {

    public static final String ID = "debug";

    public DebugRuleNode() {
        setResource(ResourceType.editor, "rule-engine/editor/common/21-debug.html");
        setResource(ResourceType.helper, "rule-engine/i18n/zh-CN/common/21-debug.html");
    }

    @Override
    public String getId() {
        return DebugRuleNode.ID;
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

