package org.jetlinks.pro.notify.network.http;

import org.jetlinks.core.Values;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class HttpNotifyTemplateTest {


    @Test
    void testCreateMessage() {

        String text = String.join("\n"
            , "POST http://iot.jetlinks.cn/notify/${#deviceId}"
            , "Content-Type: application/json"
            , ""
            , "${T(com.alibaba.fastjson.JSON).toJSONString(#this)}"
        );

        HttpNotifyTemplate template = new HttpNotifyTemplate();
        template.setText(text);

        HttpRequestMessage message = template.createMessage(Values.of(Collections.singletonMap("deviceId", "test")));

        System.out.println(message.print());

        assertEquals(message.getMethod(), HttpMethod.POST);
        assertEquals(message.getUrl(),"http://iot.jetlinks.cn/notify/test");
        assertEquals(message.getContentType(), MediaType.APPLICATION_JSON);
        assertEquals(message.payloadAsString(),  "{\"deviceId\":\"test\"}");

    }

}