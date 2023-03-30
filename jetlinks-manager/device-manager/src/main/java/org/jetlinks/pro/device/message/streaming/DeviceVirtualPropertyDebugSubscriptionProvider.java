package org.jetlinks.pro.device.message.streaming;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.streaming.Streaming;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
public class DeviceVirtualPropertyDebugSubscriptionProvider implements SubscriptionProvider {
    private final Map<String, DebugStreaming> streamingMaps = new ConcurrentHashMap<>();

    private final DevicePropertyStreamingFactory factory;

    @Override
    public String id() {
        return "virtual-property-debug";
    }

    @Override
    public String name() {
        return "虚拟属性调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/virtual-property-debug"
        };
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String id = request
            .getString("virtualId")
            .map(vId -> String.join("-", request.getAuthentication().getUser().getId(), vId))
            .orElseThrow(() -> new IllegalArgumentException("参数virtualId不能为空"));
        DebugStreaming streaming;
        Flux<?> response;

        synchronized (this) {
            if (!streamingMaps.containsKey(id)) {
                String propertyId = request
                    .getString("property")
                    .orElseThrow(() -> new IllegalArgumentException("property不能为空"));
                SimplePropertyMetadata propertyMetadata = new SimplePropertyMetadata();
                propertyMetadata.setId(propertyId);
                propertyMetadata.setName(propertyId);
                propertyMetadata.setValueType(StringType.GLOBAL);
                JSONObject virtualRule = request
                    .get("virtualRule", JSONObject.class)
                    .orElseThrow(() -> new IllegalArgumentException("virtualRule不能为空"));

                streamingMaps.put(id, streaming = new DebugStreaming(propertyId, factory.create(propertyMetadata, virtualRule)));
                response = streaming
                    .output()
                    .doFinally(s -> {
                        DebugStreaming debugStreaming = streamingMaps.remove(id);
                        if(null!=debugStreaming) {
                            debugStreaming.dispose();
                        }
                    })
                    .flatMap(msg -> Mono.justOrEmpty(DeviceMessageUtils.tryGetProperties(msg)));
            } else {
                response = Flux.empty();
                streaming = streamingMaps.get(id);
            }
        }
        long now = System.currentTimeMillis();
        String deviceId = request.getString("deviceId").orElse("debug-device");

        List<DebugProperty> properties = request
            .get("properties", JSONArray.class)
            .map(prop -> prop.toJavaList(DebugProperty.class))
            .orElse(Collections.emptyList());
        Map<String, Object> propertiesMap = new HashMap<>();

        DebugDeviceDataManager dataManager = new DebugDeviceDataManager();

        for (DebugProperty property : properties) {
            propertiesMap.put(property.getId(), property.getCurrent());
            dataManager.addProperty(deviceId, property.getId(), property.getCurrent(), now);
            //减去1秒当作上一个属性值
            dataManager.addProperty(deviceId, property.getId(), property.getLast(), now - 1000);
        }

        propertiesMap.put(DeviceDataManager.class.getName(), dataManager);

        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setProperties(propertiesMap);
        message.setDeviceId(deviceId);
        message.setTimestamp(now);

        return Flux.concat(streaming
                               .compute(message)
                               .map(val -> Collections.singletonMap(streaming.property, val)), response);
    }

    @Getter
    @Setter
    public static class DebugProperty {
        private String id;
        //当前值
        private Object current;
        //最新值
        private Object last;
    }

    @AllArgsConstructor
    private static class DebugStreaming implements Streaming<DeviceMessage, Object, DeviceMessage> {
        private final String property;
        private final Streaming<DeviceMessage, Object, DeviceMessage> streaming;

        @Override
        public Mono<Object> compute(DeviceMessage data) {
            return streaming.compute(data);
        }

        @Override
        public Flux<DeviceMessage> output() {
            return streaming.output();
        }

        @Override
        public void dispose() {
            streaming.dispose();
        }
    }
}
