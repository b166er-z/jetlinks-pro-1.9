package org.jetlinks.pro.device.web.request;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.pro.configuration.CommonConfiguration;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolDecodePayloadTest {

    @Test
    public void testHttp() {
        CommonConfiguration.load();
        ProtocolDecodePayload payload = new ProtocolDecodePayload();
        payload.setPayload(JSON.toJSONString(new HashMap<String,Object>(){
            {
                put("url","/message/publish");
                put("contentType","application/json");
                put("method","POST");
                put("body","{\"success\":true}");
            }
        }));
        payload.setPayloadType(PayloadType.JSON);
        payload.setTransport(DefaultTransport.HTTP);

        EncodedMessage message= payload.toEncodedMessage();
        assertTrue(message instanceof HttpExchangeMessage);
        HttpExchangeMessage msg = ((HttpExchangeMessage) message);
        assertEquals(msg.getUrl(),"/message/publish");
        assertEquals(msg.getPayload().toString(StandardCharsets.UTF_8),"{\"success\":true}");
        assertEquals(msg.getContentType(), MediaType.APPLICATION_JSON);
        assertEquals(msg.getMethod(), HttpMethod.POST);


    }
}