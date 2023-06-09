package org.jetlinks.pro.elastic.search.index;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.cache.Caches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConfigurationProperties(prefix = "elasticsearch.index")
public class DefaultElasticSearchIndexManager implements ElasticSearchIndexManager {

    @Getter
    @Setter
    private String defaultStrategy = "direct";

    @Getter
    @Setter
    private Map<String, String> indexUseStrategy = new HashMap<>();

    private final Map<String, ElasticSearchIndexStrategy> strategies = Caches.newCache();

    private final Map<String, ElasticSearchIndexMetadata> indexMetadataStore = Caches.newCache();

    public DefaultElasticSearchIndexManager(List<ElasticSearchIndexStrategy> strategies,
                                            @Autowired(required = false) List<ElasticSearchIndexCustomizer> customizers) {
        strategies.forEach(this::registerStrategy);
        if (customizers != null) {
            customizers.forEach(customizer -> customizer.custom(this));
        }
    }

    @Override
    public Mono<Void> putIndex(ElasticSearchIndexMetadata index) {
        return this.getIndexStrategy(index.getIndex())
            .flatMap(strategy -> strategy.putIndex(index))
            .doOnSuccess(metadata -> indexMetadataStore.put(index.getIndex(), index));
    }

    @Override
    public Mono<ElasticSearchIndexMetadata> getIndexMetadata(String index) {
        return Mono.justOrEmpty(indexMetadataStore.get(index))
            .switchIfEmpty(Mono.defer(() -> doLoadMetaData(index)
                .doOnNext(metadata -> indexMetadataStore.put(metadata.getIndex(), metadata))));
    }

    protected Mono<ElasticSearchIndexMetadata> doLoadMetaData(String index) {
        return getIndexStrategy(index)
            .flatMap(strategy -> strategy.loadIndexMetadata(index));
    }

    @Override
    public Mono<ElasticSearchIndexStrategy> getIndexStrategy(String index) {
        return Mono.justOrEmpty(strategies.get(indexUseStrategy.getOrDefault(index.toLowerCase(), defaultStrategy)))
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("[" + index + "] 不支持任何索引策略")));
    }

    @Override
    public void useStrategy(String index, String strategy) {
        indexUseStrategy.put(index, strategy);
    }

    public void registerStrategy(ElasticSearchIndexStrategy strategy) {
        strategies.put(strategy.getId(), strategy);
    }

}
