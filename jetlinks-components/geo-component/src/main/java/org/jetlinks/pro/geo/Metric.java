package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@Getter
public enum Metric {
    METERS(6378137, "m"), //米
    KILOMETERS(6378.137, "km"), //千米
    MILES(3963.191, "mi"), //英里
    FEET(20925646.325, "ft"); //英尺

    private final double multiplier;
    private final String abbreviation;


    public static Optional<Metric> of(String expr) {
        for (Metric value : values()) {
            if (value.abbreviation.equalsIgnoreCase(expr) ||
                value.name().equalsIgnoreCase(expr)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return  abbreviation;
    }
}
