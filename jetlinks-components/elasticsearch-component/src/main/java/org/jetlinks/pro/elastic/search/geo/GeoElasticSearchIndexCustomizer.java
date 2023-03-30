package org.jetlinks.pro.elastic.search.geo;

import org.jetlinks.core.metadata.types.*;
import org.jetlinks.pro.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexCustomizer;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexMetadata;
import org.springframework.stereotype.Component;

@Component
public class GeoElasticSearchIndexCustomizer implements ElasticSearchIndexCustomizer {
    static final ElasticSearchIndexMetadata metadata;

    static {
        DefaultElasticSearchIndexMetadata indexMetadata = new DefaultElasticSearchIndexMetadata(
            "geo-manager"
        );
        metadata = indexMetadata;
        indexMetadata.addProperty("id", StringType.GLOBAL);
        indexMetadata.addProperty("objectType", StringType.GLOBAL);
        indexMetadata.addProperty("shapeType", StringType.GLOBAL);
        indexMetadata.addProperty("objectId", StringType.GLOBAL);
        indexMetadata.addProperty("property", StringType.GLOBAL);
        indexMetadata.addProperty("point", GeoType.GLOBAL);
        indexMetadata.addProperty("shape", GeoShapeType.GLOBAL);
        indexMetadata.addProperty("tags", new ObjectType());
        indexMetadata.addProperty("timestamp", new DateTimeType());
    }

    @Override
    public void custom(ElasticSearchIndexManager manager) {
        //geo-manager不使用索引分片
        manager.useStrategy("geo-manager", "direct");

        manager.putIndex(metadata).block();
    }
}
