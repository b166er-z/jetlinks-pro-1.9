package org.jetlinks.pro.rule.engine.log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.pro.elastic.search.service.ElasticSearchService;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.pro.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.rule.engine.defaults.LogEvent;
import reactor.core.publisher.Mono;

import static org.jetlinks.pro.rule.engine.event.handler.RuleEngineLoggerIndexProvider.RULE_EVENT_LOG;
import static org.jetlinks.pro.rule.engine.event.handler.RuleEngineLoggerIndexProvider.RULE_LOG;

public class ElasticSearchRuleEngineLogService implements RuleEngineLogService {

    private final ElasticSearchService elasticSearchService;

    public ElasticSearchRuleEngineLogService(ElasticSearchIndexManager indexManager,
                                             ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
        indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(RULE_LOG.getIndex())
                          .addProperty("createTime", DateTimeType.GLOBAL)
                          .addProperty("timestamp", DateTimeType.GLOBAL)
                          .addProperty("level", StringType.GLOBAL)
                          .addProperty("message", StringType.GLOBAL)
                          .addProperty("nodeId", StringType.GLOBAL)
                          .addProperty("instanceId", StringType.GLOBAL))
            .then(indexManager
                      .putIndex(new DefaultElasticSearchIndexMetadata(RULE_EVENT_LOG.getIndex())
                                    .addProperty("createTime", DateTimeType.GLOBAL)
                                    .addProperty("timestamp", DateTimeType.GLOBAL)
                                    .addProperty("event", StringType.GLOBAL)
                                    .addProperty("nodeId", StringType.GLOBAL)
                                    .addProperty("ruleData", StringType.GLOBAL)
                                    .addProperty("instanceId", StringType.GLOBAL))
            )
            .subscribe();
    }


    @Subscribe("/rule-engine/*/*/event/error")
    public Mono<Void> handleEvent(TopicPayload event) {

        return elasticSearchService.commit(RULE_EVENT_LOG, RuleEngineExecuteEventInfo.of(event));
    }

    @Subscribe("/rule-engine/*/*/logger/info,warn,error")
    public Mono<Void> handleLog(LogEvent event) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(event);
        jsonObject.put("createTime",event.getTimestamp());
        return elasticSearchService.commit(RULE_LOG, jsonObject);
    }

    public Mono<PagerResult<RuleEngineExecuteEventInfo>> queryEvent(QueryParam queryParam) {
        return elasticSearchService.queryPager(RULE_EVENT_LOG, queryParam, RuleEngineExecuteEventInfo.class);
    }

    public Mono<PagerResult<RuleEngineExecuteLogInfo>> queryLog(QueryParam queryParam) {
        return elasticSearchService.queryPager(RULE_LOG, queryParam, RuleEngineExecuteLogInfo.class);
    }
}
