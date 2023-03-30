package org.jetlinks.pro.timeseries.query;

import org.jetlinks.pro.ValueObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface AggregationData extends ValueObject {

    Map<String, Object> asMap();

    @Override
    default Optional<Object> get(String name) {
        return Optional.ofNullable(asMap().get(name));
    }

    @Override
    default Map<String, Object> values() {
        return asMap();
    }

    default AggregationData merge(AggregationData another) {
        Map<String, Object> newVal = new HashMap<>(asMap());
        newVal.putAll(another.asMap());
        return of(newVal);
    }

    static AggregationData of(Map<String, Object> map) {
        return () -> map;
    }
}
