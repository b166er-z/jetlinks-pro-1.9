package org.jetlinks.pro.tdengine;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Point {

    private String metric;

    private String table;

    private Map<String, Object> values = new LinkedHashMap<>();

    private Map<String, Object> tags = new LinkedHashMap<>();

    private long timestamp;

    public Point(String metric, String table) {
        this.metric = metric;
        this.table = table;
    }

    public static Point of(String metric, String table) {
        return new Point(metric, table);
    }

    public Point timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Point tag(String metric, Object value) {
        tags.put(metric, value);
        return this;
    }

    public Point tags(Map<String, Object> values) {
        this.tags.putAll(values);
        return this;
    }

    public Point value(String metric, Object value) {
        values.put(metric, value);
        return this;
    }

    public Point values(Map<String, Object> values) {
        this.values.putAll(values);
        return this;
    }
}
