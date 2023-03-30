package org.jetlinks.pro.elastic.search.aggreation.bucket;

import lombok.*;
import org.jetlinks.pro.elastic.search.aggreation.enums.MetricsType;

import java.util.List;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BucketResponse {

    private String name;

    private List<Bucket> buckets;
}
