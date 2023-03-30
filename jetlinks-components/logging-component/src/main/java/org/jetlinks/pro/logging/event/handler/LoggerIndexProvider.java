package org.jetlinks.pro.logging.event.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.elastic.search.index.ElasticIndex;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public enum LoggerIndexProvider implements ElasticIndex {

    ACCESS("access_logger"),
    SYSTEM("system_logger");

    private String index;
}
