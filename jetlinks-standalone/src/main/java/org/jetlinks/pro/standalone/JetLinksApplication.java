package org.jetlinks.pro.standalone;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.authorization.events.AuthorizingHandleBeforeEvent;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.logging.aop.EnableAccessLogger;
import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;


@SpringBootApplication(scanBasePackages = "org.jetlinks.pro", exclude = {DataSourceAutoConfiguration.class,KafkaAutoConfiguration.class,RabbitAutoConfiguration.class,ElasticsearchRestClientAutoConfiguration.class,ElasticsearchDataAutoConfiguration.class})
@EnableCaching
@EnableEasyormRepository("org.jetlinks.pro.**.entity")
@EnableAopAuthorize
@EnableAccessLogger
//@EnableReactorQL("org.jetlinks.pro")
public class JetLinksApplication {

    public static void main(String[] args){
        SpringApplication.run(JetLinksApplication.class,args);
    }

    @Profile("dev")
    @Component
    @Slf4j
    public static class AdminAllAccess {

        @EventListener
        public void handleAuthEvent(AuthorizingHandleBeforeEvent e){
            if(e.getContext().getAuthentication().getUser().getUsername().equals("admin")){
                e.setAllow(true);
            }
        }

        @EventListener
        public void handleAccessLogger(AccessLoggerAfterEvent event){

            log.info("{}=>{} {}-{}",event.getLogger().getIp(),event.getLogger().getUrl(),event.getLogger().getDescribe(),event.getLogger().getAction());

        }
    }


}
