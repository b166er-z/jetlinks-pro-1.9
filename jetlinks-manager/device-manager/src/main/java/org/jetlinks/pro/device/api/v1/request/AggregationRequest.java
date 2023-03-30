package org.jetlinks.pro.device.api.v1.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.timeseries.query.AggregationQueryParam;
import org.jetlinks.pro.utils.TimeUtils;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AggregationRequest {

    private String interval = "1d";

    private String format = "yyyy-MM-dd";

    private String from = "now-25d";

    private String to = "now";

    private QueryParamEntity query = new QueryParamEntity();


    public AggregationQueryParam apply(AggregationQueryParam param) {
        param.setQueryParam(query);
        param.groupBy(Interval.of(interval), format)
            .from(TimeUtils.parseDate(from))
            .to(TimeUtils.parseDate(to));

        param.setLimit(query.getPageSize());

        return param;

    }
}
