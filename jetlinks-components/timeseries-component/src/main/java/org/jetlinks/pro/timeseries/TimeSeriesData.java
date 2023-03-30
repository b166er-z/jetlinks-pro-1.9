package org.jetlinks.pro.timeseries;

import org.jetlinks.pro.ValueObject;

import java.util.Date;
import java.util.Map;

public interface TimeSeriesData extends ValueObject {

    long getTimestamp();

    Map<String, Object> getData();

    @Override
    default Map<String, Object> values() {
        return getData();
    }

    static TimeSeriesData of(Date date, Map<String, Object> data) {
        return of(date == null ? System.currentTimeMillis() : date.getTime(), data);
    }

    static TimeSeriesData of(long timestamp, Map<String, Object> data) {
        return new SimpleTimeSeriesData(timestamp, data);
    }

}
