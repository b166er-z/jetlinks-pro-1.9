package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 距离: 1m,1.5km
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Distance implements Serializable {

    private double value;

    private Metric metric;

    public static Distance of(String expr) {
        char[] chars = expr.toCharArray();
        int numberIndex = 0;
        for (char c : chars) {
            if (c == '-' || c == '.' || (c >= '0' && c <= '9')) {
                numberIndex++;
            } else {
                break;
            }
        }
        BigDecimal value = new BigDecimal(chars, 0, numberIndex);
        Metric metric = Metric.of(expr.substring(numberIndex)).orElseThrow(() -> new IllegalArgumentException("不支持的格式:" + expr));
        return new Distance(value.doubleValue(), metric);
    }

    @Override
    public String toString() {
        return value + String.valueOf(metric);
    }
}
