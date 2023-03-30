package org.jetlinks.pro.simulator.core.listener;

import org.jetlinks.pro.simulator.core.SimulatorConfig;
import org.jetlinks.pro.simulator.core.SimulatorListener;

import java.util.Map;

/**
 * JSR223监听器提供商
 *
 * @author zhouhao
 * @see JSR223Listener
 * @since 1.6
 */
public class JSR223ListenerProvider implements SimulatorListenerProvider {

    @Override
    public String getType() {
        return "jsr223";
    }

    @Override
    public SimulatorListener build(SimulatorConfig.Listener listener) {

        Map<String, Object> conf = listener.getConfiguration();

        //脚本语言，支持js,groovy
        String lang = (String) conf.getOrDefault("lang", "js");
        //脚本内容
        String script = (String) conf.get("script");

        return new JSR223Listener(listener.getId(), lang, script);
    }
}
