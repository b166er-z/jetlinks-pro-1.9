package org.jetlinks.pro.plugin;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;

@Getter
@Setter
public class PluginDescription {

    private String name;

    private String description;

    private int version;

    private List<PropertyMetadata> startParameterMetadata;
}
