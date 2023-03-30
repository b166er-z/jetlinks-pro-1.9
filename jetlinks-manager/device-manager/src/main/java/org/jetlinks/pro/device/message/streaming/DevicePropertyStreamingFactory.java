package org.jetlinks.pro.device.message.streaming;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.pro.streaming.Streaming;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Component
public class DevicePropertyStreamingFactory {

    private final Map<String, BiFunction<PropertyMetadata, Map<String, Object>, Streaming<DeviceMessage, Object, DeviceMessage>>> supports = new ConcurrentHashMap<>();

    public DevicePropertyStreamingFactory() {
        //脚本支持
        supports.put("script", (property, config) -> {
            String script = (String) config.get("script");
            Assert.hasText(script, "script不能为空");
            return ScriptDevicePropertyStreaming.create(script);
        });

        //窗口支持
        supports.put("window", (property, config) -> {

            WindowStreamingFactory factory = FastBeanCopier
                .copy(config, new WindowStreamingFactory());
            factory.setProperty(property.getId());
            return factory.create();
        });
    }


    public Streaming<DeviceMessage, Object, DeviceMessage> create(PropertyMetadata property, Map<String, Object> rule) {

        String type = String.valueOf(rule.get("type"));
        return Optional
            .ofNullable(supports.get(type))
            .map(builder -> builder.apply(property,rule))
            .orElseThrow(() -> new UnsupportedOperationException("不支持的规则类型:" + type));

    }

}
