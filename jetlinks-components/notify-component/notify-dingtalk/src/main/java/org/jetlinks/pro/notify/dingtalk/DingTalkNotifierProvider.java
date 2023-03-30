package org.jetlinks.pro.notify.dingtalk;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.ConfigMetadataConstants;
import org.jetlinks.pro.notify.*;
import org.jetlinks.pro.notify.template.TemplateManager;
import org.jetlinks.pro.notify.template.TemplateProperties;
import org.jetlinks.pro.notify.template.TemplateProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.Nonnull;

@Component
public class DingTalkNotifierProvider implements NotifierProvider, TemplateProvider {

    private final WebClient client;

    private final TemplateManager templateManager;

    public DingTalkNotifierProvider(TemplateManager templateManager, WebClient.Builder builder) {
        this.templateManager = templateManager;
        this.client=builder.build();
    }

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("通知配置", "")
        .add("appKey", "appKey", "", new StringType().expand(ConfigMetadataConstants.required.value(true)))
        .add("appSecret", "appSecret", "", new StringType());

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("模版配置", "")
        .add("agentId", "应用ID", "", new StringType().expand(ConfigMetadataConstants.required.value(true)))
        .add("userIdList", "收信人ID", "与部门ID不能同时为空", new StringType())
        .add("departmentIdList", "收信部门ID", "与收信人ID不能同时为空", new StringType())
        .add("toAllUser", "全部用户", "推送到全部用户", new BooleanType())
        .add("message", "内容", "最大不超过500字", new StringType().expand(ConfigMetadataConstants.maxLength.value(500L)));

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.dingTalk;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return DingTalkProvider.dingTalkMessage;
    }

    @Override
    public Mono<DingTalkMessageTemplate> createTemplate(TemplateProperties properties) {
        return Mono.fromSupplier(() -> {
            return ValidatorUtils.tryValidate(JSON.parseObject(properties.getTemplate(), DingTalkMessageTemplate.class));
        });
    }

    @Nonnull
    @Override
    public Mono<DingTalkNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.defer(() -> {
            DingTalkProperties dingTalkProperties = FastBeanCopier.copy(properties.getConfiguration(), new DingTalkProperties());
            return Mono.just(new DingTalkNotifier(properties.getId(), client, ValidatorUtils.tryValidate(dingTalkProperties), templateManager));
        });
    }

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return templateConfig;
    }
}
