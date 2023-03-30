package org.jetlinks.pro.openapi.interceptor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.ReactiveAuthenticationSupplier;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.context.ContextKey;
import org.hswebframework.web.context.ContextUtils;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.logger.ReactiveLogger;
import org.hswebframework.web.utils.ReactiveWebUtils;
import org.jetlinks.pro.openapi.OpenApiClient;
import org.jetlinks.pro.openapi.OpenApiClientManager;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@AllArgsConstructor
public class OpenApiFilter implements WebFilter, ReactiveAuthenticationSupplier {

    private final OpenApiClientManager clientManager;

    public OpenApiFilter(OpenApiClientManager clientManager) {
        this.clientManager = clientManager;
        ReactiveAuthenticationHolder.addSupplier(this);
    }

    @Getter
    @Setter
    private boolean timestampCheck = true;

    @Getter
    @Setter
    private Duration timestampMaxInterval = Duration.ofMinutes(5);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientId = exchange.getRequest().getHeaders().getOrEmpty("X-Client-Id").stream().findFirst().orElse(null);
        if (StringUtils.hasText(clientId)) {
            String sign = exchange.getRequest().getHeaders().getOrEmpty("X-Sign").stream().findFirst().orElse(null);
            String timestamp = exchange.getRequest().getHeaders().getOrEmpty("X-Timestamp").stream().findFirst().orElse(null);
            if (StringUtils.isEmpty(sign)) {
                return Mono.error(new ValidationException("请求头X-Sign不能为空"));
            }
            if (StringUtils.isEmpty(timestamp)) {
                return Mono.error(new ValidationException("请求头X-Timestamp不能为空"));
            }
            if (!checkTimestamp(timestamp)) {
                return Mono.error(new ValidationException("请求头X-Timestamp错误"));
            }
            return clientManager
                .getClient(clientId)
                .switchIfEmpty(Mono.error(new NotFoundException("Client不存在")))
                .flatMap(client -> {
                    ServerHttpRequest request = exchange.getRequest();

                    //过滤IP白名单
                    String ip = ReactiveWebUtils.getIpAddr(request);
                    if (!client.verifyIpAddress(ip)) {
                        return Mono.error(new AccessDenyException("拒绝访问"));
                    }

                    MediaType mediaType = request.getHeaders().getContentType();
                    OpenApiWebExchangeDecorator exchangeDecorator = new OpenApiWebExchangeDecorator(exchange);
                    Mono<Boolean> checker = Mono.just(true);

                    if (request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.DELETE) {
                        checker = checkSign(sign, timestamp, client, exchangeDecorator.getRequest().getQueryParams().toSingleValueMap());
                    } else if (mediaType != null && mediaType.includes(MediaType.APPLICATION_FORM_URLENCODED)) {
                        checker = exchangeDecorator.getFormData()
                            .map(MultiValueMap::toSingleValueMap)
                            .flatMap(kv -> checkSign(sign, timestamp, client, kv));
                    } else if (request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) {
                        checker = exchangeDecorator.getRequest()
                            .getBody()
                            .as(buffer -> checkSign(sign, timestamp, client, buffer));
                    }
                    return checker
                        .flatMap(success -> {
                            if (!success) {
                                return Mono.error(new ValidationException("签名错误"));
                            }
                            return chain.filter(exchangeDecorator);
                        })
                        .subscriberContext(Context.of(OpenApiClient.class, client))
                        .subscriberContext(ContextUtils.acceptContext(ctx -> ctx.put(ContextKey.of(Authentication.class), client.getAuthentication())))
                        .then(ReactiveLogger.mdc("userId", client.getAuthentication().getUser().getId()))
                        .then(ReactiveLogger.mdc("username", client.getAuthentication().getUser().getName()));
                })
                .subscriberContext(ReactiveLogger.start("openApiClientId", clientId));


        }
        return chain.filter(exchange);
    }

    private boolean checkTimestamp(String ts) {
        try {
            long time = Long.parseLong(ts);
            return System.currentTimeMillis() - time < timestampMaxInterval.toMillis();
        } catch (Exception ignore) {

        }
        return true;
    }

    private Mono<Boolean> checkSign(String sign, String timestamp, OpenApiClient client, Flux<DataBuffer> payload) {
        return Mono.defer(() -> {
            MessageDigest digest = client.getSignature().getMessageDigest();

            return payload
                .doOnNext(buffer -> digest.update(buffer.asByteBuffer()))
                .then(Mono.fromSupplier(() -> {
                    digest.update(timestamp.getBytes());
                    digest.update(client.getSecureKey().getBytes());
                    return sign.equalsIgnoreCase(Hex.encodeHexString(digest.digest()));
                }));
        });
    }

    private Mono<Boolean> checkSign(String sign, String timestamp, OpenApiClient client, Map<String, String> multiValueMap) {

        return Mono.fromSupplier(() -> {
            MessageDigest digest = client.getSignature().getMessageDigest();
            byte[] param = new TreeMap<>(multiValueMap)
                .entrySet()
                .stream()
                .map(e -> e.getKey().concat("=").concat(e.getValue()))
                .collect(Collectors.joining("&")).getBytes();
            digest.update(param);
            digest.update(timestamp.getBytes());
            digest.update(client.getSecureKey().getBytes());
            return sign.equalsIgnoreCase(Hex.encodeHexString(digest.digest()));
        });
    }

    @Override
    public Mono<Authentication> get(String userId) {
        return Mono.empty();
    }

    @Override
    public Mono<Authentication> get() {
        return ContextUtils.reactiveContext()
            .flatMap(ctx -> Mono.justOrEmpty(ctx.get(Authentication.class)));
    }
}
