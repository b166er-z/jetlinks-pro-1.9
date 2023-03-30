package org.jetlinks.pro.elastic.search.embedded;

import lombok.SneakyThrows;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.transport.Netty4Plugin;
import org.jetlinks.pro.elastic.search.configuration.GeoShapeMapperPlugin;

import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class EmbeddedElasticSearch extends Node {

    static {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    @SneakyThrows
    public EmbeddedElasticSearch(EmbeddedElasticSearchProperties properties) {
        super(InternalSettingsPreparer.prepareEnvironment(
            properties.applySetting(
                Settings.builder()
                    .put("node.name", "test")
                    .put("discovery.type", "single-node")
                    .put("transport.type", Netty4Plugin.NETTY_TRANSPORT_NAME)
                    .put("http.type", Netty4Plugin.NETTY_HTTP_TRANSPORT_NAME)
                    .put("network.host", "0.0.0.0")
                    .put("http.port", 9200)
            ).build(), Collections.emptyMap(), null, () -> "default"),
           Arrays.asList(GeoShapeMapperPlugin.class,Netty4Plugin.class), false);
    }

}
