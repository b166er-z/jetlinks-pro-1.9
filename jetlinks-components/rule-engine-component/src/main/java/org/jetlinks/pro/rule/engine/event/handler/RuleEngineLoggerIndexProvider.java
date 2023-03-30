package org.jetlinks.pro.rule.engine.event.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.elastic.search.index.ElasticIndex;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public enum RuleEngineLoggerIndexProvider implements ElasticIndex {

    RULE_LOG("rule-engine-execute-log", "_doc"),
    RULE_EVENT_LOG("rule-engine-execute-event", "_doc");

    private final String index;

    private final String type;
}
