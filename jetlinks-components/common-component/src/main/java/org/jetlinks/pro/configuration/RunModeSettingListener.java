package org.jetlinks.pro.configuration;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

public class RunModeSettingListener implements ApplicationListener<ApplicationPreparedEvent> {
    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {

        Environment environment = event.getApplicationContext().getEnvironment();

        RunMode.mode = environment.getProperty("jetlinks.mode", "cluster");


    }
}
