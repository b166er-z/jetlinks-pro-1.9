package org.jetlinks.pro.network.http.executor;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.http.DefaultHttpResponseMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP响应与规则数据转换器.
 * <p>
 * 在规则引擎中使用HTTP请求{@link HttpRequestTaskExecutorProvider}时,在下游节点将收到对应的数据；
 * <p>
 * 在HTTP响应{@link HttpServerResponseTaskExecutorProvider}中,上游节点通过返回指定格式的数据来完成HTTP请求响应.数据格式:
 * <pre>
 * {
 *   headers:{header:value},
 *   status: 200,
 *   contentType:"application/json",
 *   payload:"http response body"
 * }
 * </pre>
 *
 * @author bsetfeng
 * @since 1.0
 **/
public class HttpResponseMessageCodec implements RuleDataCodec<HttpResponseMessage> {


    public static final HttpResponseMessageCodec instance = new HttpResponseMessageCodec();

    static {
        RuleDataCodecs.register(HttpResponseMessage.class, instance);
    }

    static void register() {
    }

    @Override
    public Object encode(HttpResponseMessage data, Feature... features) {
        PayloadType payloadType = Feature.find(PayloadType.class, features)
                                         .orElse(PayloadType.JSON);
        return doEncode(data, payloadType);
    }

    public static Map<String, Object> doEncode(HttpResponseMessage data, PayloadType payloadType) {

        Map<String, Object> map = new HashMap<>();
        if (data.getContentType() == MediaType.APPLICATION_JSON) {
            map.put("payload", PayloadType.JSON.read(data.getPayload()));
        } else if (data.getContentType() == MediaType.APPLICATION_OCTET_STREAM) {
            map.put("payload", PayloadType.BINARY.read(data.getPayload()));
        } else {
            map.put("payload", PayloadType.STRING.read(data.getPayload()));
        }

        map.put("headers", data.getHeaders()
                               .stream()
                               .collect(Collectors
                                            .toMap(Header::getName,
                                                   header -> String.join(",", header.getValue()),
                                                   (v1, v2) -> String.join(",", v1, v2))));

        map.put("status", data.getStatus());
        if (data.getContentType() != null) {
            map.put("contentType", data.getContentType().toString());
        }
        return map;
    }

    @Override
    public Flux<? extends HttpResponseMessage> decode(RuleData data, Feature... features) {
        return data
            .dataToMap()
            .flatMap(map -> {
                ValueObject valueObject = ValueObject.of(map);

                int status = Optional.ofNullable(map.get("status"))
                                     .map(String::valueOf)
                                     .map(Integer::parseInt)
                                     .orElse(200);

                Object payload = valueObject.get("payload").orElse(null);

                List<Header> headers = valueObject
                    .get("headers")
                    .map(conf -> {
                        if (conf instanceof Map) {
                            return ((Map<String, Object>) conf)
                                .entrySet()
                                .stream()
                                .map(entry -> new Header(entry.getKey(), new String[]{String.valueOf(entry.getValue())}))
                                .collect(Collectors.toList());
                        }
                        if (conf instanceof Collection) {
                            return ((Collection<?>) conf)
                                .stream()
                                .map(header -> FastBeanCopier.copy(header, new Header()))
                                .collect(Collectors.toList());
                        }
                        return null;
                    }).orElse(Collections.emptyList());

                DefaultHttpResponseMessage message = new DefaultHttpResponseMessage();
                message.setHeaders(headers);
                message.setStatus(status);
                MediaType contentType = valueObject
                    .getString("contentType")
                    .map(MediaType::parseMediaType)
                    .orElseGet(() -> message
                        .getHeader("Content-Type")
                        .map(header -> header.getValue()[0])
                        .map(MediaType::parseMediaType)
                        .orElse(MediaType.APPLICATION_JSON));
                message.setContentType(contentType);

                if (payload == null) {
                    message.setPayload(Unpooled.EMPTY_BUFFER);
                } else if (payload instanceof String) {
                    message.setPayload(Unpooled.wrappedBuffer(((String) payload).getBytes()));
                } else {
                    if (contentType.includes(MediaType.APPLICATION_FORM_URLENCODED) && payload instanceof Map) {
                        message.setPayload(Unpooled.wrappedBuffer(HttpUtils
                                                                      .createEncodedUrlParams(((Map) payload))
                                                                      .getBytes()));
                    } else {
                        message.setPayload(Unpooled.wrappedBuffer(JSON.toJSONBytes(payload)));
                    }
                }

                return Mono.just(message);
            });
    }
}
