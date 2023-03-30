package org.jetlinks.pro.rule.engine.editor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractEditorNode implements EditorNode {

    private final Map<ResourceType, Resource> resources = new HashMap<>();

    public AbstractEditorNode(){

    }

    public void setResource(ResourceType type, String resourceLocation) {
        setResource(type, new ClassPathResource(resourceLocation, ClassUtils.getDefaultClassLoader()));
    }

    public void setResource(ResourceType type, Resource resourceLocation) {
        resources.put(type, resourceLocation);
    }

    @Override
    public Set<String> getTypes() {
        return Collections.singleton(getId());
    }

    @Override
    public Map<ResourceType, Resource> getEditorResources() {
        return resources;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public abstract int getOrder();
}
