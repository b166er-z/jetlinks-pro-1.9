package org.jetlinks.pro.notify.voice;

import org.jetlinks.pro.notify.template.TemplateManager;
import org.jetlinks.pro.notify.voice.aliyun.AliyunNotifierProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VoiceNotifierConfiguration {


    @Bean
    @ConditionalOnBean(TemplateManager.class)
    public AliyunNotifierProvider aliyunNotifierProvider(TemplateManager templateManager) {
        return new AliyunNotifierProvider(templateManager);
    }

}
