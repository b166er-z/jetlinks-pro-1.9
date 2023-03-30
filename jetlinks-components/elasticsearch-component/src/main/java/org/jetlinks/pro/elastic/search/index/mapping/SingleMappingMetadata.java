package org.jetlinks.pro.elastic.search.index.mapping;

import lombok.*;
import org.jetlinks.pro.elastic.search.enums.ElasticDateFormat;
import org.jetlinks.pro.elastic.search.enums.ElasticPropertyType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SingleMappingMetadata {

    private String name;

    private ElasticDateFormat format;

    private ElasticPropertyType type;
}
