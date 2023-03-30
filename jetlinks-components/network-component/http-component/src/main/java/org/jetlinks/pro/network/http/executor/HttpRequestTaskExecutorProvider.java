package org.jetlinks.pro.network.http.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.jetlinks.core.message.codec.http.*;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.security.Certificate;
import org.jetlinks.pro.network.security.CertificateManager;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * HTTP 请求规则节点执行器提供商,用于在规则引擎中发起http请求
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
@AllArgsConstructor
@EditorResource(
    id = "http request",
    name = "HTTP请求",
    editor = "rule-engine/editor/network/21-httprequest.html",
    helper = "rule-engine/i18n/zh-CN/network/21-httprequest.html",
    order = 150
)
public class HttpRequestTaskExecutorProvider implements TaskExecutorProvider {

    private final CertificateManager certificateManager;

    @Override
    public String getExecutor() {
        return "http-request";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new HttpRequestTaskExecutor(context));
    }

    //OAuth2.0认证方式
    enum GrantType {
        password {
            @Override
            Map<String, String> getParams(ValueObject configs) {
                Map<String, String> map = new LinkedHashMap<>();

                String username = configs.getString("user").orElseThrow(() -> new IllegalArgumentException("未设置user"));
                String password = configs
                    .getString("password")
                    .orElseThrow(() -> new IllegalArgumentException("未设置password"));
                map.put("username", username);
                map.put("password", password);

                return map;
            }
        },
        client_credentials {
            @Override
            Map<String, String> getParams(ValueObject configs) {
                Map<String, String> map = new LinkedHashMap<>();

                String clientId = configs
                    .getString("client_id")
                    .orElseThrow(() -> new IllegalArgumentException("未设置client_id"));
                String clientSecret = configs
                    .getString("client_secret")
                    .orElseThrow(() -> new IllegalArgumentException("未设置client_id"));
                map.put("client_id", clientId);
                map.put("client_secret", clientSecret);

                return map;
            }
        };

        abstract Map<String, String> getParams(ValueObject configs);

        Mono<JSONObject> requestToken(WebClient.RequestHeadersSpec<?> headersSpec,
                                      ValueObject configs,
                                      HttpRequestTaskExecutor executor) {
            String tokenUrl = configs
                .getString("tokenUrl")
                .orElseThrow(() -> new IllegalArgumentException("未设置tokenUrl"));
            Map<String, String> authParam = getParams(configs);

            AuthInsertType authType = configs.getString("authInsertType")
                                             .map(AuthInsertType::valueOf)
                                             .orElse(AuthInsertType.body);
            BodyType bodyType = configs.getString("bodyType")
                                       .map(BodyType::valueOf)
                                       .orElse(BodyType.formBody);

            Map<String, Object> body = new HashMap<>();
            body.put("grant_type", name());
            configs.get("scope").ifPresent(val -> body.put("scope", val));

            WebClient.RequestBodySpec spec = executor
                .client
                .post()
                .uri(tokenUrl);
            if (authType == AuthInsertType.header) {
                spec.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encodeBase64String((String.join(":", authParam.values()))
                                                                                                .getBytes()));
            } else {
                body.putAll(authParam);
            }
            bodyType.apply(spec, body);

            return spec
                .exchange()
                .flatMap(response -> {
                    if (response.statusCode().isError()) {
                        return response
                            .releaseBody()
                            .then(
                                Mono.error(new RuntimeException("request oauth2 token error:" + response
                                    .statusCode()
                                    .getReasonPhrase()))
                            );
                    }
                    boolean isJson = response.headers().contentType()
                                             .filter(MediaType.APPLICATION_JSON::includes)
                                             .isPresent();
                    Mono<JSONObject> result;

                    if (isJson) {
                        result = response
                            .bodyToMono(Map.class)
                            .map(JSONObject::new);
                    } else {
                        result = response
                            .bodyToMono(String.class)
                            .map(m -> {
                                if (m.startsWith("{")) {
                                    return JSON.parseObject(m);
                                }
                                return new JSONObject((Map) HttpUtils.parseEncodedUrlParams(m));
                            });
                    }
                    return result
                        .doOnNext(json -> {
                            String accessToken = json.getString("access_token");
                            if (StringUtils.isEmpty(accessToken)) {
                                throw new IllegalArgumentException("OAuth2响应格式错误:" + json);
                            }
                        });
                });
        }

        enum BodyType {
            formBody {
                @Override
                void apply(WebClient.RequestBodySpec sepc, Map<String, Object> params) {
                    sepc.contentType(MediaType.APPLICATION_FORM_URLENCODED);
                    LinkedMultiValueMap<String, String> valueMap = new LinkedMultiValueMap<>();
                    params.forEach((k, v) -> valueMap.put(k, Collections.singletonList(String.valueOf(v))));
                    sepc.body(BodyInserters.fromFormData(valueMap));
                }
            },
            jsonBody {
                @Override
                void apply(WebClient.RequestBodySpec sepc, Map<String, Object> params) {
                    sepc.contentType(MediaType.APPLICATION_JSON);
                    sepc.body(BodyInserters.fromValue(params));
                }
            };

            abstract void apply(WebClient.RequestBodySpec sepc, Map<String, Object> params);
        }

        enum AuthInsertType {
            header,
            body;
        }
    }

    //授权类型
    enum AuthType {
        basic {
            @Override
            Mono<Void> apply(WebClient.RequestHeadersSpec<?> headersSpec,
                             ValueObject configs,
                             HttpRequestTaskExecutor executor) {
                String username = configs.getString("user").orElse("");
                String password = configs.getString("password").orElse("");
                headersSpec.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encodeBase64String((username + ":" + password)
                                                                                                       .getBytes()));
                return Mono.empty();
            }
        },
        oauth2 {
            @Override
            Mono<Void> apply(WebClient.RequestHeadersSpec<?> headersSpec,
                             ValueObject valueObject,
                             HttpRequestTaskExecutor executor) {

                String token = (String) executor.cache.get("access_token");
                Long expires = (Long) executor.cache.get("expires");
                Long requestTime = (Long) executor.cache.get("requestTime");

                if (StringUtils.hasText(token)
                    && expires != null
                    && requestTime != null
                    && (System.currentTimeMillis() - requestTime > 0)) {
                    headersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    return Mono.empty();
                } else {
                    GrantType grantType = valueObject.getString("grantType")
                                                     .map(GrantType::valueOf)
                                                     .orElse(GrantType.client_credentials);

                    return grantType
                        .requestToken(headersSpec, valueObject, executor)
                        .doOnNext(json -> {
                            String accessToken = json.getString("access_token");
                            if (StringUtils.isEmpty(accessToken)) {
                                throw new IllegalArgumentException("OAuth2响应格式错误:" + json);
                            }
                            executor.cache.put("access_token", accessToken);
                            executor.cache.put("expires", json.getLongValue("expires_in"));
                            executor.cache.put("requestTime", System.currentTimeMillis());
                            headersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                        })
                        .then();
                }
            }
        },
        digest {
            @Override
            Mono<Void> apply(WebClient.RequestHeadersSpec<?> headersSpec,
                             ValueObject configs,
                             HttpRequestTaskExecutor executor) {
                // TODO: 2020/6/24
                return Mono.empty();
            }
        },
        bearer {
            @Override
            Mono<Void> apply(WebClient.RequestHeadersSpec<?> headersSpec,
                             ValueObject configs,
                             HttpRequestTaskExecutor executor) {
                String token = configs.getString("token", configs.getString("password").orElse(""));
                headersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                return Mono.empty();
            }
        };

        abstract Mono<Void> apply(WebClient.RequestHeadersSpec<?> headersSpec,
                                  ValueObject valueObject,
                                  HttpRequestTaskExecutor executor);
    }

    private static final DataBufferFactory bufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));

    class HttpRequestTaskExecutor extends FunctionTaskExecutor {

        private WebClient client;

        private HttpMethod method;

        private String url;

        private AuthType authType;

        private Map<String, String> cookies;

        private Certificate certificate;

        private boolean useTls = false;

        private boolean trustAll = true;

        private int maxHeaderLength = 10 * 1024;

        private final int maxBuffer = 20 * 1024 * 1024;

        private Duration connectTimeout;
        private Duration soTimeout;

        private final Map<String, Object> cache = new ConcurrentHashMap<>();

        public HttpRequestTaskExecutor(ExecutionContext context) {
            super("HTTP Request", context);
            reload();
        }

        private void initConfig() {
            //初始化配置
            ValueObject configs = ValueObject.of(context.getJob().getConfiguration());
            method = configs.getString("method")
                            .map(String::toUpperCase)
                            .map(HttpMethod::resolve)
                            .orElse(null);

            authType = configs.getString("authType")
                              .map(AuthType::valueOf)
                              .orElse(null);

            url = configs.getString("url").orElse(null);

            connectTimeout = configs.getDuration("connectTimeout", Duration.ofSeconds(10));
            soTimeout = configs.getDuration("soTimeout", Duration.ofSeconds(30));
            maxHeaderLength = configs.getInt("maxHeaderLength", 10 * 1024);

            if (useTls = configs.getBoolean("useTls", false)) {
                trustAll = configs.getBoolean("trustAll", false);
                configs.getString("tls")
                       .ifPresent(tlsId -> certificateManager
                           .getCertificate(tlsId)
                           .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("TLS证书不存在")))
                           .doOnError(er -> context.getLogger().error(er.getMessage(), er))
                           .subscribe(s -> certificate = s));
            }
        }

        @Override
        public void validate() {
            super.validate();
            initConfig();
        }

        @Override
        public void reload() {
            initConfig();
            cache.clear();
            client = WebClient
                .builder()
                .baseUrl(url)
                .apply(builder -> {
                    builder.codecs(clientCodecConfigurer -> clientCodecConfigurer
                        .defaultCodecs()
                        .maxInMemorySize(maxBuffer));
                    TcpClient tcpClient = TcpClient.create();

                    if (!connectTimeout.isNegative()) {
                        tcpClient = tcpClient
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()));
                    }

                    if (!soTimeout.isNegative()) {
                        tcpClient = tcpClient
                            .doOnConnected(connection -> connection
                                .addHandlerLast(new ReadTimeoutHandler(soTimeout.toMillis(), TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(soTimeout.toMillis(), TimeUnit.MILLISECONDS)));
                    }

                    reactor.netty.http.client.HttpClient client = HttpClient
                        .from(tcpClient)
                        .httpResponseDecoder(spec -> spec
                            .maxInitialLineLength(maxHeaderLength)
                            .maxHeaderSize(maxHeaderLength));

                    if (useTls) {
                        client = client
                            .secure(t -> {
                                if (certificate != null) {
                                    t.sslContext(SslContextBuilder
                                                     .forClient()
                                                     .keyManager(certificate.getKeyManagerFactory())
                                                     .trustManager(trustAll ? InsecureTrustManagerFactory.INSTANCE : certificate
                                                         .getTrustManagerFactory())
                                    );
                                } else {
                                    t.sslContext(SslContextBuilder.forClient()
                                                                  .trustManager(trustAll ? InsecureTrustManagerFactory.INSTANCE : null));
                                }
                            });
                    }
                    builder.clientConnector(new ReactorClientHttpConnector(client));
                })
                .build();
        }

        @Override
        public void shutdown() {
            super.shutdown();
            cache.clear();
        }

        @SneakyThrows
        private String encodeUrl(String url) {
            return URLEncoder.encode(url, "utf-8");
        }

        private Mono<HttpResponseMessage> doRequest(HttpRequestMessage request, Map<String, Object> upstreamData) {
            String url = request.getUrl();
            if (!StringUtils.hasText(url)) {
                url = "";
            }
            if (StringUtils.hasText(url) && !url.startsWith("http://") && !url.startsWith("https://")) {
                url = this.url + url;
            }
            if (!CollectionUtils.isEmpty(request.getQueryParameters())) {
                String querys = request
                    .getQueryParameters()
                    .entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + encodeUrl(String.valueOf(e.getValue())))
                    .collect(Collectors.joining("&"));
                if (url.contains("?")) {
                    url = "&" + querys;
                } else {
                    url = "?" + querys;
                }
            }

            Map<String, Object> vals = new HashMap<>(context.getJob().getConfiguration());
            vals.putAll(upstreamData);
            ValueObject values = ValueObject.of(vals);
            HttpMethod method = this.method != null ? this.method : request.getMethod();

            //  GET /api/
            WebClient.RequestBodySpec requestBodySpec = client
                .method(method)
                .uri(url, upstreamData);

            //  headers
            for (Header header : request.getHeaders()) {
                requestBodySpec.header(header.getName(), header.getValue());
            }
            // auth header
            WebClient.RequestHeadersSpec<?> spec = requestBodySpec;
            Mono<Void> before = authType != null ? authType.apply(spec, values, this) : Mono.empty();
            // body
            if (method != HttpMethod.GET && method != HttpMethod.DELETE) {
                if (MediaType.APPLICATION_FORM_URLENCODED.includes(request.getContentType())) {
                    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
                    if (!CollectionUtils.isEmpty(request.getRequestParam())) {
                        request.getRequestParam().forEach(multiValueMap::add);
                    }
                    spec = requestBodySpec
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(multiValueMap));

                } else {
                    spec = requestBodySpec
                        .contentType(request.getContentType() == null ? MediaType.APPLICATION_JSON : request.getContentType())
                        .body(BodyInserters.fromDataBuffers(Mono.just(bufferFactory.wrap(request.payloadAsBytes()))));
                }
            }
            @SuppressWarnings("all")
            Map<String, String> cookies = (Map<String, String>) upstreamData.getOrDefault("cookies", this.cookies);

            if (!CollectionUtils.isEmpty(cookies)) {
                spec.cookies(map -> {
                    cookies.forEach(map::add);
                });
            }

            return before
                .then(spec
                          .exchange()
                          .flatMap(response -> {
                              if (response.rawStatusCode() == 401) {
                                  cache.clear();
                              }
                              SimpleHttpResponseMessage msg = new SimpleHttpResponseMessage();
                              msg.setContentType(request.getContentType());
                              msg.setStatus(response.rawStatusCode());
                              msg.setHeaders(response
                                                 .headers()
                                                 .asHttpHeaders()
                                                 .entrySet()
                                                 .stream()
                                                 .map(e -> new Header(e.getKey(), e.getValue().toArray(new String[0])))
                                                 .collect(Collectors.toList()));
                              return response
                                  .bodyToMono(ByteBuffer.class)
                                  .map(Unpooled::wrappedBuffer)
                                  .defaultIfEmpty(Unpooled.EMPTY_BUFFER)
                                  .map(payload -> {
                                      msg.setPayload(payload);
                                      return msg;
                                  });
                          }));

        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            return input.dataToMap()
                        .flatMap(map -> doRequest(HttpRequestMessageCodec.fromMap(map), map)
                            .flatMap(response -> Mono
                                .justOrEmpty(RuleDataCodecs
                                                 .getCodec(HttpResponseMessage.class)
                                                 .map(codec -> codec.encode(response)))
                                .map(val -> context.newRuleData(input.newData(val)))));
        }

    }

}
