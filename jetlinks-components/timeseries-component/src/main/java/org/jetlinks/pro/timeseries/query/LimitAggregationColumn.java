package org.jetlinks.pro.timeseries.query;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class LimitAggregationColumn extends AggregationColumn {

    private int limit;

    public LimitAggregationColumn(String property,
                                  String alias,
                                  Aggregation aggregation,
                                  int limit) {
        super(property, alias, aggregation);
        this.limit = limit;
    }
}
