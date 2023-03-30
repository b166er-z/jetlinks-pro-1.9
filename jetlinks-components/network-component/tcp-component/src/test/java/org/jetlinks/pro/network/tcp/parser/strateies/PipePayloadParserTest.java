package org.jetlinks.pro.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.pro.network.utils.BytesUtils;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

class PipePayloadParserTest {


    public static void main(String[] args) {

        Flux.just(1, 2, 3)
            .publishOn(Schedulers.elastic())
            .map(i -> {

                return "test-" + i;
            })
            .subscribe((i) -> {
                System.out.println(Thread.currentThread());
            });

    }

    @Test
    void testDirect() {
        PipePayloadParser parser = new PipePayloadParser();

        parser.direct(Function.identity());


        parser.handlePayload()
            .doOnSubscribe(sb -> {
                Mono.delay(Duration.ofMillis(100))
                    .subscribe(r -> {
                        parser.handle(Buffer.buffer("123"));
                    });
            }).take(1)
            .map(bf -> bf.toString(StandardCharsets.UTF_8))
            .as(StepVerifier::create)
            .expectNext("123")
            .verifyComplete();
    }

    @Test
    void testSplicingUnpack() {
        PipePayloadParser parser = new PipePayloadParser();

        parser.fixed(4)
            .handler(buffer -> {
                int len = BytesUtils.leToInt(buffer.getBytes());
                parser.fixed(len);
            })
            .handler(buffer -> parser.result(buffer).complete());


        parser.handlePayload()
            .doOnSubscribe(sb -> {
                Mono.delay(Duration.ofMillis(100))
                    .subscribe(r -> {
                        Buffer buffer = Buffer.buffer(BytesUtils.intToLe(5));
                        buffer.appendString("1234");
                        parser.handle(buffer);
                        parser.handle(Buffer.buffer("5"));

                        parser.handle(Buffer.buffer(new byte[]{0, 0}));
                        parser.handle(Buffer.buffer(new byte[]{0, 6}).appendString("12"));
                        parser.handle(Buffer.buffer("3456"));
                    });
            })
            .take(2)
            .map(bf -> bf.toString(StandardCharsets.UTF_8))
            .as(StepVerifier::create)
            .expectNext("12345", "123456")
            .verifyComplete();
    }

    @Test
    void testTopSailiotUnpack() {
        String test = "7470736C0300680138363737323530333337353139323030" +
            "01040000000A02040000003C030F3138302E3130312E3134372E313135040435363833" +
            "06022710070236B008021388090100" +
            "0A04101010100B04010101010C1034363031313330303631343831333430" +
            "0D05434D4E45544C38696F74";

        String test1="7470736c01001d0138363432383730333832373439343830"+
            "190124161530641e014030005923"+
            "696f74";

        PipePayloadParser parser = new PipePayloadParser();
        parser.fixed(7)
            .handler(buffer -> {
                byte[] bytes = buffer.getBytes();
                int bodyLen = org.jetlinks.core.utils.BytesUtils.beToInt(bytes, 5, 2);
                int len = bodyLen + 5;
                System.out.println(len);
                parser.fixed(len).result(buffer);
            })
            .handler(buffer -> parser.result(buffer).complete());

        parser.handlePayload()
            .doOnSubscribe(sb->{
                Mono.delay(Duration.ofMillis(1))
                    .subscribe(r->{
                        byte[] bytes1 = new byte[0];
                        byte[] bytes=new byte[0];
                        try {
                            bytes1 = Hex.decodeHex(test1);
                            bytes=Hex.decodeHex(test);
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }
                        Buffer buffer = Buffer.buffer(bytes1);
                        Buffer buffer1 = Buffer.buffer(bytes);
                        parser.handle(buffer);
                        parser.handle(buffer1);
                    });
            })
            .take(1)
            .map(bf-> Hex.encodeHexString(bf.getBytes()))
            .as(StepVerifier::create)
            .expectNext()
            .expectNext(test1)
            .verifyComplete();
    }

    @Test
    void test() {
        PipePayloadParser parser = new PipePayloadParser();

        parser.fixed(4)
            .handler(buffer -> {
                int len = BytesUtils.beToInt(buffer.getBytes());
                parser.fixed(len);
            })
            .handler(buffer -> {
                parser.result(buffer)
                    .complete();
            });

        byte[] payload = "hello".getBytes();

        Buffer buffer = Buffer.buffer(payload.length + 4);

        buffer.appendBytes(BytesUtils.intToBe(payload.length));
        buffer.appendBytes(payload);

        parser.handlePayload()
            .doOnSubscribe(sb -> {
                Flux.range(0, 100)
                    .delayElements(Duration.ofMillis(10))
                    .subscribe(i -> {
                        parser.handle(buffer);
                    });
            })
            .take(2)
            .map(bf -> bf.toString(StandardCharsets.UTF_8))
            .as(StepVerifier::create)
            .expectNext("hello", "hello")
            .verifyComplete();


    }


}