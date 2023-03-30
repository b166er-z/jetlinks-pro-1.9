package org.jetlinks.pro.protocol;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.utils.BytesUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 脚本协议加载器提供商，用于提供使用脚本来进行协议解析的支持
 * <p/>
 * <pre>
 *   //解码消息
 *  codec.decoder(function (context) {
 *
 *     var message = context.getMessage();
 *
 *     if(message.getTopic()==='/test'){
 *         return {
 *             "messageType":"EVENT",
 *             "event":"test",
 *             "data":"test"
 *         }
 *     }
 *
 *
 *     return null;
 *
 * });
 *
 * //编码读取属性指令
 * codec.encoder("READ_PROPERTY",function(context){
 *
 * });
 * </pre>
 */
@Component
public class ScriptProtocolSupportLoaderProvider implements ProtocolSupportLoaderProvider {
    @Override
    public String getProvider() {
        return "script";
    }

    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {

        return Mono.fromCallable(() -> {
            Map<String, Object> config = definition.getConfiguration();

            Object transport = config.get("transport");
            String lang = (String) config.get("lang");
            String script = (String) config.get("script");
            String protocol = (String) config.get("protocol");
            Assert.hasText(lang, "lang can not be empty");
            Assert.hasText(script, "script can not be empty");
            Assert.notNull(transport, "transport can not be empty");
            Assert.hasText(protocol, "protocol can not be empty");


            DynamicScriptEngine engine = Optional
                .ofNullable(DynamicScriptEngineFactory.getEngine(lang))
                .orElse(DynamicScriptEngineFactory.getEngine("js"));
            String scriptId = DigestUtils.md5Hex(script);

            if (!engine.compiled(scriptId)) {
                engine.compile(scriptId, script);
            }

            CustomDeviceMessageEncoder encoder = new CustomDeviceMessageEncoder();
            CustomDeviceMessageDecoder decoder = new CustomDeviceMessageDecoder();

            ScriptHelper helper = new ScriptHelper(encoder, decoder);

            Map<String, Object> scriptContext = new HashMap<>();

            scriptContext.put("codec", helper);

            engine.execute(scriptId, scriptContext);

            Transport[] transports = Stream
                .of(transport)
                .flatMap(trans -> {
                    if (trans instanceof Collection) {
                        return ((Collection<?>) trans).stream();
                    }
                    return Stream.of(String.valueOf(trans).split("[,]"));
                }).map(String::valueOf)
//                .map(String::toUpperCase)
                .map(DefaultTransport::valueOf)
                .toArray(Transport[]::new);

            CompositeProtocolSupport support = new CompositeProtocolSupport();
            for (Transport transport1 : transports) {
                CompositeDeviceMessageCodec codec = new CompositeDeviceMessageCodec(
                    transport1, encoder, decoder
                );
                support.addMessageCodecSupport(codec);
            }

            support.setMetadataCodec(new JetLinksDeviceMetadataCodec());
            support.setId(protocol);
            support.setName(definition.getName());
            support.setDescription(definition.getDescription());

            return support;
        });
    }

    @AllArgsConstructor
    public static class ScriptHelper extends BytesUtils {
        CustomDeviceMessageEncoder encoder;
        CustomDeviceMessageDecoder decoder;

        public ScriptHelper encoder(String type, Function<MessageEncodeContext, Object> encoder) {
            this.encoder.encoder(type, encoder);
            return this;
        }

        public ScriptHelper decoder(Function<MessageDecodeContext, Object> encoder) {
            this.decoder.decoder(encoder);
            return this;
        }

        public ByteBuf newBuffer(byte[] data) {
            return Unpooled.wrappedBuffer(data);
        }

        public ByteBuf newBuffer(Object data) {
            return newBuffer(JSON.toJSONString(data));
        }

        public ByteBuf newBuffer(String data) {
            return Unpooled.wrappedBuffer(data.getBytes());
        }

        public SimpleMqttMessage newMqtt() {
            return new SimpleMqttMessage();
        }

        public DefaultCoapMessage newCoap() {
            return new DefaultCoapMessage();

        }

    }


}
