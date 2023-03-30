package org.jetlinks.pro.rule.engine.editor;

import lombok.AllArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@AllArgsConstructor
public class DefaultEditorNodeManager implements EditorNodeManager {

    private final ObjectProvider<EditorNodeProvider> providers;

    @Override
    public Flux<EditorNode> getNodes() {
        return Flux
            .fromIterable(providers)
            .flatMap(EditorNodeProvider::getNodes);
    }


}
