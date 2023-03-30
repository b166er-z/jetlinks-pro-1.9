package org.jetlinks.pro.notify.network.http;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.ConfigMetadataConstants;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.notify.*;
import org.jetlinks.pro.notify.network.NetworkNotifyProvider;
import org.jetlinks.pro.notify.network.mqtt.MqttNotifyTemplate;
import org.jetlinks.pro.notify.template.Template;
import org.jetlinks.pro.notify.template.TemplateManager;
import org.jetlinks.pro.notify.template.TemplateProperties;
import org.jetlinks.pro.notify.template.TemplateProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AllArgsConstructor
@Component
public class HttpNotifierProvider implements NotifierProvider, TemplateProvider {

    private final NetworkManager networkManager;

    private final TemplateManager templateManager;

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.network;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return NetworkNotifyProvider.HTTP_CLIENT;
    }

    @Override
    public Mono<HttpNotifyTemplate> createTemplate(TemplateProperties properties) {
        return Mono.just(JSON.parseObject(properties.getTemplate(), HttpNotifyTemplate.class));
    }

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata()
        .add("networkId", "网络组件", "", new StringType()
            .expand("selector", "network")
            .expand("networkType", DefaultNetworkType.HTTP_CLIENT.name())
        );

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata()
        .add("httpText", "HTTP消息", String.join("\n"
            , "POST http://[host]:[port]/api"
            , "Content-Type: application"
            , ""
            , "${#data[body]}"
        ), new StringType().expand(ConfigMetadataConstants.maxLength, 100 * 1024L));

    @Nullable
    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return templateConfig;
    }

    @Nonnull
    @Override
    public Mono<HttpNotifier> createNotifier(@Nonnull NotifierProperties properties) {

        String networkId = properties.getString("networkId").orElseThrow(() -> new IllegalArgumentException("[networkId]不能为空"));

        return Mono.just(new HttpNotifier(properties.getId(), networkId, networkManager, templateManager));
    }
}
