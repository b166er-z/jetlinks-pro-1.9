package org.jetlinks.pro.notify.network.mqtt;

import org.jetlinks.core.Values;
import org.jetlinks.core.message.codec.MqttMessage;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttNotifyTemplateTest {

    @Test
    void testCreateMessage() {

        String text = String.join("\n"
            , "QoS1 /device/${#deviceId}"
            , ""
            , "${T(com.alibaba.fastjson.JSON).toJSONString(#this)}"
        );

        MqttNotifyTemplate template = new MqttNotifyTemplate();
        template.setText(text);

        MqttMessage message = template.createMessage(Values.of(Collections.singletonMap("deviceId", "test")));

        System.out.println(message.print());

        assertEquals(message.getQosLevel(), 1);
        assertEquals(message.getTopic(),"/device/test");
        assertEquals(message.payloadAsString(),  "{\"deviceId\":\"test\"}");

    }
}