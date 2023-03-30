package org.jetlinks.pro.device.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DevicePropertyAggregationData {

    private String property;

    private String time;

    private Object value;

    private String formatValue;
}
