package org.jetlinks.pro.elastic.search.index.strategies;

import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.pro.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 按月对来划分索引策略
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class TimeByMonthElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    public TimeByMonthElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("time-by-month", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now();
        String idx = wrapIndex(index);
        return idx + "_" + now.getYear() + "-" + now.getMonthValue();
    }
}
