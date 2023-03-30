package org.jetlinks.pro.elastic.search.index;

import lombok.*;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.common.settings.Settings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "elasticsearch.index.settings")
public class ElasticSearchIndexProperties {

    private int numberOfShards = 1;

    private int numberOfReplicas = 0;

    private int totalFieldsLimit = 2000;

    private Map<String, String> options;

    public Settings toSettings() {

        Settings.Builder builder = Settings.builder();
        if(MapUtils.isNotEmpty(options)){
            options.forEach(builder::put);
        }
        return builder
                       .put("number_of_shards", Math.max(1, numberOfShards))
                       .put("number_of_replicas", numberOfReplicas)
                       .put("mapping.total_fields.limit", totalFieldsLimit)
                       .build();
    }
}
