package org.jetlinks.pro.protocol;

import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessageDecodeContext;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import reactor.test.StepVerifier;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScriptProtocolSupportLoaderProviderTest {



    @Test
    @SneakyThrows
    void test() {
        ScriptProtocolSupportLoaderProvider provider = new ScriptProtocolSupportLoaderProvider();

        ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
        definition.setId("test");
        definition.setName("测试");
        definition.setProvider("script");

        Map<String, Object> map = new HashMap<>();
        map.put("lang", "js");
        map.put("script", StreamUtils.copyToString(new ClassPathResource("test.js").getInputStream(), StandardCharsets.UTF_8));
        map.put("transport", "MQTT");
        map.put("protocol", "test1.0");
        definition.setConfiguration(map);

        ProtocolSupport protocolSupport = provider.load(definition).block();

        Assert.assertNotNull(protocolSupport);

        protocolSupport
                .getMessageCodec(DefaultTransport.MQTT)
                .flatMapMany(codec -> codec.decode(new MessageDecodeContext() {
                    @Override
                    public EncodedMessage getMessage() {
                        return SimpleMqttMessage.builder()
                                .topic("/test")
                                .payload(Unpooled.wrappedBuffer("test".getBytes()))
                                .build()
                                ;
                    }

                    @Override
                    public DeviceOperator getDevice() {
                        return null;
                    }
                }))
                .cast(EventMessage.class)
                .as(StepVerifier::create)
                .expectNextMatches(msg -> msg.getEvent().equals("test") && msg.getData().equals("test"))
                .verifyComplete();


    }

    @Test
    @SneakyThrows
    void testHttp() {
        ScriptProtocolSupportLoaderProvider provider = new ScriptProtocolSupportLoaderProvider();

        ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
        definition.setId("test-http");
        definition.setName("测试");
        definition.setProvider("script");

        Map<String, Object> map = new HashMap<>();
        map.put("lang", "js");
        map.put("script", StreamUtils.copyToString(new ClassPathResource("test1.js").getInputStream(), StandardCharsets.UTF_8));
        map.put("transport", "HTTP");
        map.put("protocol", "test-http1.0");
        definition.setConfiguration(map);

        ProtocolSupport protocolSupport = provider.load(definition).block();

        Assert.assertNotNull(protocolSupport);

        protocolSupport
            .getMessageCodec(DefaultTransport.HTTP)
            .flatMapMany(codec -> codec.decode(new MessageDecodeContext() {
                @Override
                @Nonnull
                public EncodedMessage getMessage() {
                    //模拟HTTP请求
                    return MockHttpExchangeMessage
                        .builder()
                        .contentType(MediaType.APPLICATION_JSON)
                        .payload(Unpooled.wrappedBuffer("{}".getBytes()))
                        .url("/10045219/device-event")
                        .build()
                        ;
                }

                @Override
                public DeviceOperator getDevice() {
                    return null;
                }
            }))
            .cast(ReportPropertyMessage.class)
            .as(StepVerifier::create)
            .expectNextMatches(msg -> msg.getProperties().containsKey("property"))
            .verifyComplete();


    }

}