package org.jetlinks.pro.timeseries;

import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;

public interface TimeSeriesMetadata {

    TimeSeriesMetric getMetric();

    List<PropertyMetadata> getProperties();

}
