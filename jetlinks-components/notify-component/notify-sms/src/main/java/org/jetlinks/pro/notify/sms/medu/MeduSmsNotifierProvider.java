package org.jetlinks.pro.notify.sms.medu;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.notify.*;
import org.jetlinks.pro.notify.sms.SmsProvider;
import org.jetlinks.pro.notify.template.TemplateManager;
import org.jetlinks.pro.notify.template.TemplateProperties;
import org.jetlinks.pro.notify.template.TemplateProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 漫道短信通知服务
 * </a>
 *
 * @author zoubin
 * @since 1.9
 */
@Component
@Slf4j
@AllArgsConstructor
public class MeduSmsNotifierProvider implements NotifierProvider, TemplateProvider {

    private final TemplateManager templateManager;

    @Nonnull
    @Override
    public Provider getProvider() {
        return SmsProvider.meduSms;
    }

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("漫道短信模版",
        "")
            .add("signName", "签名", "", new StringType())
            .add("code", "模版编码", "", new StringType())
            .add("phoneNumber", "收信人", "", new StringType());

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("漫道API配置"
        ,"")
            .add("regionId", "regionId", "regionId", new StringType())
            .add("accessKeyId", "accessKeyId", "", new StringType())
            .add("secret", "secret", "", new StringType());

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return templateConfig;
    }

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Override
    public Mono<MeduSmsTemplate> createTemplate(TemplateProperties properties) {
        return Mono.fromCallable(() -> ValidatorUtils.tryValidate(JSON.parseObject(properties.getTemplate(), MeduSmsTemplate.class)));
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.sms;
    }

    @Nonnull
    @Override
    public Mono<MeduSmsNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.fromSupplier(() -> new MeduSmsNotifier(properties, templateManager));
    }
}
