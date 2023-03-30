package org.jetlinks.pro.device.features;

import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.springframework.stereotype.Component;

@Component
public class DevicePropertySumMapFeature extends DevicePropertyAggMapFeature {

    public DevicePropertySumMapFeature(DeviceDataService dataService) {
        super("device.property.sum", Aggregation.SUM, dataService::aggregationPropertiesByDevice);
    }

}
