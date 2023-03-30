package org.jetlinks.pro.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import org.jetlinks.core.Values;
import org.jetlinks.core.utils.BytesUtils;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.tcp.parser.PayloadParser;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class ScriptPayloadParserBuilderTest {

    @Test
    void testSplicingUnpack() {
        ScriptPayloadParserBuilder builder = new ScriptPayloadParserBuilder();
        Map<String, Object> config = new HashMap<>();
        config.put("script", "\n" +
            "var BytesUtils = org.jetlinks.core.utils.BytesUtils;\n" +
            "parser.fixed(4)\n" +
            "       .handler(function(buffer){\n" +
            "            var len = BytesUtils.beToInt(buffer.getBytes(),2,2);\n" +
            "            parser.fixed(len).result(buffer);\n" +
            "        })\n" +
            "       .handler(function(buffer){\n" +
            "            parser.result(buffer)\n" +
            "                   .complete();\n" +
            "        });");
        config.put("lang", "javascript");
        System.out.println(config.get("script"));
        PayloadParser parser = builder.build(ValueObject.of(config));

        parser.handlePayload()
            .doOnSubscribe(sb -> {
                Mono.delay(Duration.ofMillis(100))
                    .subscribe(r -> {
                        Buffer buffer = Buffer.buffer();
                        buffer.appendBytes(BytesUtils.shortToBe((short) 5));
                        buffer.appendString("1234");
                        parser.handle(Buffer.buffer(new byte[]{0, 0}));
                        parser.handle(buffer);
                        parser.handle(Buffer.buffer("5"));

                        parser.handle(Buffer.buffer(new byte[]{0, 0}));
                        parser.handle(Buffer.buffer(BytesUtils.shortToBe((short) 5)).appendString("12"));
                        parser.handle(Buffer.buffer("345"));
                    });
            })
            .take(2)
            .map(bf -> bf.toString(StandardCharsets.UTF_8))
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNext("\u0000\u000012345", "\u0000\u000012345")
            .verifyComplete();
    }

}