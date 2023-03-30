package org.jetlinks.pro.rule.engine.model.nodes;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;

@Component
@EditorResource(
    id = "comment",
    name = "注释",
    editor = "rule-engine/editor/common/90-comment.html",
    helper = "rule-engine/i18n/zh-CN/common/90-comment.html",
    order = 60
)
public class CommentNodeConverter  implements NodeConverter{
    @Override
    public String getNodeType() {
        return "comment";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        return null;
    }
}
