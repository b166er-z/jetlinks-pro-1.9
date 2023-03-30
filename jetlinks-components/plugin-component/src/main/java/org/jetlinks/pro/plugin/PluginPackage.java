package org.jetlinks.pro.plugin;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class PluginPackage implements Serializable {

    private static final long serialVersionUID = -6849794470754667710L;

    private String id;

    private String name;

    private Map<String, Object> loaderConfigs;

    private Map<String, Object> providerConfigs;

}
