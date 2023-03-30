package org.jetlinks.pro.configuration;

import io.netty.buffer.ByteBuf;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CommonConfigurationTest {

    static {
        CommonConfiguration.load();
    }

    @Test
    void testByteBuf() {
        ByteBuf buf = (ByteBuf) BeanUtilsBean
            .getInstance()
            .getConvertUtils()
            .convert("abc", ByteBuf.class);

        assertEquals(buf.toString(StandardCharsets.UTF_8), "abc");
    }

    @Test
    void testMediaType() {
        MediaType type = (MediaType) BeanUtilsBean
            .getInstance()
            .getConvertUtils()
            .convert("application/json", MediaType.class);

        assertTrue(type.includes(MediaType.APPLICATION_JSON));
    }
}