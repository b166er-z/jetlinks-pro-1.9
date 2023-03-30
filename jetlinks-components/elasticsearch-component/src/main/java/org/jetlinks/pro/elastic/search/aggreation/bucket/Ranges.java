package org.jetlinks.pro.elastic.search.aggreation.bucket;

import lombok.Getter;
import lombok.Setter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class Ranges {

    private String key;

    private Object form;

    private Object to;
}
