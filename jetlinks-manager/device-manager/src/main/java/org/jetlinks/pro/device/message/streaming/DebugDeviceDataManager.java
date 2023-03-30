package org.jetlinks.pro.device.message.streaming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.DeviceDataManager;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DebugDeviceDataManager implements DeviceDataManager {
    private final Map<String, Map<String, LinkedList<DebugPropertyValue>>> properties = new LinkedHashMap<>();

    private long firstTimestamp;
    private long lastTimestamp;

    public void addProperty(String deviceId, String property, Object value, long timestamp) {
        if (timestamp < firstTimestamp || firstTimestamp == 0) {
            firstTimestamp = timestamp;
        }
        if (timestamp > lastTimestamp) {
            lastTimestamp = timestamp;
        }
        LinkedList<DebugPropertyValue> values = properties
            .computeIfAbsent(deviceId, ignore -> new ConcurrentHashMap<>())
            .computeIfAbsent(property, ignore -> new LinkedList<>());
        if (null != value) {
            values.add(new DebugPropertyValue(value, timestamp));
            Collections.sort(values);
        }
    }

    @Override
    public Mono<PropertyValue> getLastProperty(String deviceId, String propertyId) {
        return getLastProperty(deviceId, propertyId, System.currentTimeMillis());
    }

    private Optional<LinkedList<DebugPropertyValue>> getProperties(String deviceId, String propertyId) {
        return Optional.ofNullable(properties.get(deviceId))
                       .map(map -> map.get(propertyId));
    }

    @Override
    public Mono<PropertyValue> getLastProperty(String deviceId, String propertyId, long baseTime) {

        return Mono
            .justOrEmpty(this
                             .getProperties(deviceId, propertyId)
                             .map(list -> {
                                 Iterator<DebugPropertyValue> iterator = list.descendingIterator();
                                 while (iterator.hasNext()) {
                                     DebugPropertyValue next = iterator.next();
                                     if (next.getTimestamp() < baseTime) {
                                         return next;
                                     }
                                 }
                                 return null;
                             })
            );
    }

    @Override
    public Mono<PropertyValue> getFistProperty(String deviceId, String propertyId) {
        return Mono
            .justOrEmpty(this
                             .getProperties(deviceId, propertyId)
                             .map(LinkedList::getFirst)
            );
    }

    @Override
    public Mono<Long> getLastPropertyTime(String deviceId, long baseTime) {
        if (lastTimestamp == 0) {
            return Mono.empty();
        }
        if (lastTimestamp < baseTime) {
            return Mono.just(lastTimestamp);
        }

        return Mono.justOrEmpty(
            Optional
                .ofNullable(properties.get(deviceId))
                .map(map -> {
                    long temp = 0;
                    for (LinkedList<DebugPropertyValue> value : map.values()) {
                        Iterator<DebugPropertyValue> values = value.descendingIterator();
                        while (values.hasNext()) {
                            DebugPropertyValue val = values.next();
                            long time = val.getTimestamp();
                            if (time < baseTime && temp < time) {
                                temp = time;
                            }
                        }
                    }
                    if (temp != 0) {
                        return temp;
                    }
                    return null;
                })
        );
    }

    @Override
    public Mono<Long> getFirstPropertyTime(String deviceId) {
        if (firstTimestamp == 0) {
            return Mono.empty();
        }
        return Mono.just(firstTimestamp);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class DebugPropertyValue implements PropertyValue, Comparable<DebugPropertyValue> {
        private Object value;
        private long timestamp;

        @Override
        public int compareTo(DebugPropertyValue o) {
            return Long.compare(timestamp, o.timestamp);
        }
    }
}
