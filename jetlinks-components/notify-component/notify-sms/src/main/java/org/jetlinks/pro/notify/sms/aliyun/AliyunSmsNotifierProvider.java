package org.jetlinks.pro.notify.sms.aliyun;

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
 * 阿里云短信通知服务
 * </a>
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
@AllArgsConstructor
public class AliyunSmsNotifierProvider implements NotifierProvider, TemplateProvider {

    private final TemplateManager templateManager;

    @Nonnull
    @Override
    public Provider getProvider() {
        return SmsProvider.aliyunSms;
    }

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("阿里云短信模版",
        "https://help.aliyun.com/document_detail/108086.html")
            .add("signName", "签名", "", new StringType())
            .add("code", "模版编码", "", new StringType())
            .add("phoneNumber", "收信人", "", new StringType());

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("阿里云API配置"
        ,"https://help.aliyun.com/document_detail/101300.html")
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
    public Mono<AliyunSmsTemplate> createTemplate(TemplateProperties properties) {
        return Mono.fromCallable(() -> ValidatorUtils.tryValidate(JSON.parseObject(properties.getTemplate(), AliyunSmsTemplate.class)));
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.sms;
    }

    @Nonnull
    @Override
    public Mono<AliyunSmsNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.fromSupplier(() -> new AliyunSmsNotifier(properties, templateManager));
    }
}
