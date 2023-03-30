package org.jetlinks.pro.protocol.local;

import lombok.SneakyThrows;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.spi.ServiceContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class LocalFileProtocolSupportTest {

    @Test
    @SneakyThrows
    void test(){

        LocalFileProtocolSupport support=new LocalFileProtocolSupport();

        support.init(new File("../../dev/demo-protocol/target/classes"), new ServiceContext() {
            @Override
            public Optional<Value> getConfig(ConfigKey<String> key) {
                return Optional.empty();
            }

            @Override
            public Optional<Value> getConfig(String key) {
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> getService(Class<T> service) {
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> getService(String service) {
                return Optional.empty();
            }

            @Override
            public <T> List<T> getServices(Class<T> service) {
                return Collections.emptyList();
            }

            @Override
            public <T> List<T> getServices(String service) {
                return Collections.emptyList();
            }
        },"org.jetlinks.demo.protocol.DemoProtocolSupportProvider");


        Thread.sleep(1000000);

    }

}