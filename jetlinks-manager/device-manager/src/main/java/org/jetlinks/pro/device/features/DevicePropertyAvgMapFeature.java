package org.jetlinks.pro.device.features;

import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.springframework.stereotype.Component;

@Component
public class DevicePropertyAvgMapFeature extends DevicePropertyAggMapFeature {

    public DevicePropertyAvgMapFeature(DeviceDataService dataService) {
        super("device.property.avg", Aggregation.AVG, dataService::aggregationPropertiesByDevice);
    }

}
