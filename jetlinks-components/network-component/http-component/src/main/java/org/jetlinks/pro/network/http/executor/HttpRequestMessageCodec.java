package org.jetlinks.pro.network.http.executor;

import lombok.SneakyThrows;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.pro.network.http.DefaultHttpRequestMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequestMessageCodec implements RuleDataCodec<HttpRequestMessage> {

    private static final HttpRequestMessageCodec instance = new HttpRequestMessageCodec();

    static {
        RuleDataCodecs.register(HttpRequestMessage.class, instance);
    }

    static void register() {
    }

    @Override
    @SneakyThrows
    public Object encode(HttpRequestMessage data, Feature... features) {

        Object payload = data.parseBody();

        Map<String, Object> map = new HashMap<>();
        map.put("contentType", String.valueOf(data.getContentType()));
        map.put("url", data.getUrl());
        map.put("path", data.getPath());
        map.put("method", data.getMethod());
        map.put("headers", data.getHeaders()
            .stream()
            .collect(Collectors.toMap(Header::getName, header -> String.join(",", header.getValue()), (v1, v2) -> String.join(",", v1, v2))));

        map.put("queryParameters", data.getQueryParameters());
        map.put("payload", payload);
        return map;
    }

    @Override
    public Flux<HttpRequestMessage> decode(RuleData data, Feature... features) {

        return data
            .dataToMap()
            .flatMap(map -> Mono.just(fromMap(map)));
    }

    @SuppressWarnings("all")
    public static HttpRequestMessage fromMap(Map<String, Object> map) {
        String url = (String) map.getOrDefault("url", map.get("path"));
        String method = String.valueOf(map.getOrDefault("method", "GET"));
        String contentType = (String) map.get("contentType");

        Map<String, Object> headers = (Map<String, Object>) map.get("headers");
        Map<String, String> queryParameters = (Map<String, String>) map.get("queryParameters");

        Object body = map.get("payload");

        DefaultHttpRequestMessage message = new DefaultHttpRequestMessage();
        message.setUrl(url);
        message.setMethod(HttpMethod.resolve(method));

        if (StringUtils.hasText(contentType)) {
            message.setContentType(MediaType.valueOf(contentType));
        }

        if (!CollectionUtils.isEmpty(queryParameters)) {
            message.setQueryParameters(queryParameters);
            if (body == null) {
                body = HttpUtils.createEncodedUrlParams(queryParameters);
            }
        }
        if (!CollectionUtils.isEmpty(headers)) {
            message.setHeaders(
                headers.entrySet().stream()
                    .map(e -> new Header(e.getKey(), new String[]{String.valueOf(e.getValue())}))
                    .collect(Collectors.toList())
            );
        }

        message.setBody(body);


        return message;
    }

}
