package org.jetlinks.pro.rule.engine.editor.annotation;

import org.jetlinks.pro.rule.engine.editor.AbstractEditorNode;
import org.jetlinks.pro.rule.engine.editor.ResourceType;
import org.springframework.util.StringUtils;

public class AnnotationEditorNode extends AbstractEditorNode {

    private final EditorResource resource;

    public AnnotationEditorNode(EditorResource resource) {
        this.resource = resource;
        setResource(ResourceType.editor, resource.editor());
        if (StringUtils.hasText(resource.helper())) {
            setResource(ResourceType.helper, resource.helper());
        }
    }

    @Override
    public String getId() {
        return resource.id();
    }

    @Override
    public String getName() {
        return resource.name();
    }

    @Override
    public int getOrder() {
        return resource.order();
    }
}
