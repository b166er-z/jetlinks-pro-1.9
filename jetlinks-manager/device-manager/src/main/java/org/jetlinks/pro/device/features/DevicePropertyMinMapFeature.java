package org.jetlinks.pro.device.features;

import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.springframework.stereotype.Component;

@Component
public class DevicePropertyMinMapFeature extends DevicePropertyAggMapFeature {

    public DevicePropertyMinMapFeature(DeviceDataService dataService) {
        super("device.property.min", Aggregation.MIN, dataService::aggregationPropertiesByDevice);
    }

}
