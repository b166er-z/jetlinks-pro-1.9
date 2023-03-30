package org.jetlinks.pro.device.web.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.device.service.data.DeviceDataService;

import java.util.List;

@Getter
@Setter
public class AggRequest {
    private List<DeviceDataService.DevicePropertyAggregation> columns;

    private DeviceDataService.AggregationRequest query;
}