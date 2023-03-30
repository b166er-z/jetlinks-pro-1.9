package org.jetlinks.pro.network.tcp.parser.strateies;

import com.google.common.escape.Escapers;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetlinks.core.Values;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.tcp.parser.PayloadParser;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FixLengthPayloadParserBuilderTest {



    @Test
    void testFixLength() {
        FixLengthPayloadParserBuilder builder = new FixLengthPayloadParserBuilder();
        PayloadParser parser = builder.build(ValueObject.of(Collections.singletonMap("size", 5)));
        List<String>  arr = new ArrayList<>();

        parser.handlePayload()
            .map(buffer -> buffer.toString(StandardCharsets.UTF_8))
            .subscribe(arr::add);

        parser.handle(Buffer.buffer("123"));
        parser.handle(Buffer.buffer("4567"));
        parser.handle(Buffer.buffer("890"));

        Assert.assertArrayEquals(arr.toArray(),new Object[]{
            "12345","67890"
        });

    }

    @Test
    void testDelimited() {
        DelimitedPayloadParserBuilder builder = new DelimitedPayloadParserBuilder();
        PayloadParser parser = builder.build(ValueObject.of(Collections.singletonMap("delimited", "@@")));
        List<String>  arr = new ArrayList<>();

        parser.handlePayload()
            .map(buffer -> buffer.toString(StandardCharsets.UTF_8))
            .subscribe(arr::add);

        parser.handle(Buffer.buffer("123"));
        parser.handle(Buffer.buffer("45@@67"));
        parser.handle(Buffer.buffer("890@@111"));

        Assert.assertArrayEquals(arr.toArray(),new Object[]{
            "12345","67890"
        });

    }

    @Test
    void testDelimitedUnicode() {
        DelimitedPayloadParserBuilder builder = new DelimitedPayloadParserBuilder();
        PayloadParser parser = builder.build(ValueObject.of(Collections.singletonMap("delimited", "\\u0055\\u00aa")));
        List<String>  arr = new ArrayList<>();

        parser.handlePayload()
            .map(buffer -> buffer.toString(StandardCharsets.UTF_8))
            .subscribe(arr::add);

        parser.handle(Buffer.buffer("123"));
        parser.handle(Buffer.buffer().appendBytes(new byte[]{0x55,(byte) 0xaa}));
        parser.handle(Buffer.buffer("456"));
        parser.handle(Buffer.buffer().appendBytes(new byte[]{0x55,(byte) 0xaa}));

        Assert.assertArrayEquals(arr.toArray(),new Object[]{
            "123","456"
        });
    }

    @Test
    void testDelimitedEscape() {
        DelimitedPayloadParserBuilder builder = new DelimitedPayloadParserBuilder();
        PayloadParser parser = builder.build(ValueObject.of(Collections.singletonMap("delimited", "\\r")));
        List<String>  arr = new ArrayList<>();

        parser.handlePayload()
            .map(buffer -> buffer.toString(StandardCharsets.UTF_8))
            .subscribe(arr::add);

        parser.handle(Buffer.buffer("123"));
        parser.handle(Buffer.buffer("45\r67"));
        parser.handle(Buffer.buffer("890\r111"));

        Assert.assertArrayEquals(arr.toArray(),new Object[]{
            "12345","67890"
        });

    }
}