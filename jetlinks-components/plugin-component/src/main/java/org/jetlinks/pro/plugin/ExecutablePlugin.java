package org.jetlinks.pro.plugin;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.pro.ValueObject;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ExecutablePlugin extends Plugin {

    List<PropertyMetadata> getParameterMetadata();

    PropertyMetadata getResponseMetadata();

    Flux<Object> execute(ValueObject parameters);

}
