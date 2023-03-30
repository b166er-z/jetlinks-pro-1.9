package org.jetlinks.pro.rule.engine.editor;

import lombok.AllArgsConstructor;
import org.jetlinks.pro.rule.engine.editor.annotation.AnnotationEditorNode;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResources;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@AllArgsConstructor
public class SpringBeanEditorNodeProvider implements EditorNodeProvider , BeanPostProcessor {

    private final List<EditorNode> nodes = new CopyOnWriteArrayList<>();

    @Override
    public Flux<EditorNode> getNodes() {
        return Flux.fromIterable(nodes);
    }
    public void register(EditorResource resource) {
        nodes.add(new AnnotationEditorNode(resource));
    }

    public void register(EditorNode editorNode) {
        nodes.add(editorNode);
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {

        EditorResource ann = AnnotationUtils.findAnnotation(ClassUtils.getUserClass(bean), EditorResource.class);
        if (null != ann) {
            register(ann);
        }
        EditorResources anns = AnnotationUtils.findAnnotation(ClassUtils.getUserClass(bean), EditorResources.class);
        if (null != anns) {
            for (EditorResource resource : anns.resources()) {
                register(resource);
            }
        }
        if(bean instanceof EditorNode){
            register(((EditorNode) bean));
        }

        return bean;
    }
}
