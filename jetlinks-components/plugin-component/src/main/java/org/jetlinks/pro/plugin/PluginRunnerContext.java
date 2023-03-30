package org.jetlinks.pro.plugin;

import java.util.List;
import java.util.Optional;

public interface PluginRunnerContext {

    <T> Optional<T> getService(Class<T> service);

    <T> Optional<T> getService(String service);

    <T> List<T> getServices(Class<T> service);


}
