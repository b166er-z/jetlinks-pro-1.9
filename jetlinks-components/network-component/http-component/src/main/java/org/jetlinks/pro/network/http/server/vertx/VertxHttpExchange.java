package org.jetlinks.pro.network.http.server.vertx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.SocketAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.pro.network.http.DefaultHttpRequestMessage;
import org.jetlinks.pro.network.http.VertxWebUtils;
import org.jetlinks.pro.network.http.server.HttpExchange;
import org.jetlinks.pro.network.http.server.HttpRequest;
import org.jetlinks.pro.network.http.server.HttpResponse;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认HTTP交换消息
 *
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@Getter
@Setter
@Slf4j
public class VertxHttpExchange implements HttpExchange, HttpResponse, HttpRequest {

    private final HttpServerRequest httpServerRequest;

    private final NetMonitor netMonitor;

    private final HttpServerResponse response;

    private final Mono<ByteBuf> body;

    private final String requestId;

    public VertxHttpExchange(HttpServerRequest httpServerRequest,
                             HttpServerConfig config,
                             NetMonitor netMonitor) {

        this.httpServerRequest = httpServerRequest;
        this.response = httpServerRequest.response();
        this.netMonitor = netMonitor;
        this.requestId = UUID.randomUUID().toString();
        config.getHttpHeaders().forEach(response::putHeader);

        if (httpServerRequest.method() == HttpMethod.GET) {
            body = Mono.just(Unpooled.EMPTY_BUFFER);
        } else {
            body = Mono.<ByteBuf>create(sink -> {
                if (httpServerRequest.isEnded()) {
                    sink.success();
                } else {
                    httpServerRequest
                        .bodyHandler(buffer -> sink.success(buffer.getByteBuf()));
                }
            }).cache();
            body.subscribe();
        }
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public long timestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public HttpRequest request() {
        return this;
    }


    @Override
    public HttpResponse response() {
        return this;
    }

    @Override
    public boolean isClosed() {
        return response.closed() || response.ended();
    }

    @Override
    public HttpResponse status(int status) {
        response.setStatusCode(status);
        return this;
    }

    private Map<String, String> convertRequestParam(MultiMap multiMap) {
        return multiMap.entries()
                       .stream()
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> String.join(",", a, b)));
    }

    private List<Header> convertHeader(MultiMap multiMap) {
        return multiMap.entries()
                       .stream()
                       .map(entry -> {
                           Header header = new Header();
                           header.setName(entry.getKey());
                           header.setValue(new String[]{entry.getValue()});
                           return header;
                       })
                       .collect(Collectors.toList())
            ;
    }

    private org.springframework.http.HttpMethod convertMethodType(HttpMethod method) {
        for (org.springframework.http.HttpMethod httpMethod : org.springframework.http.HttpMethod.values()) {
            if (httpMethod.toString().equals(method.toString())) {
                return httpMethod;
            }
        }
        throw new UnsupportedOperationException("不支持的HttpMethod类型: " + method);
    }

    private void setResponseDefaultLength(int length) {
        if (!isClosed()) {
            response.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        }
    }

    @Override
    public HttpResponse contentType(MediaType mediaType) {
        if (null != mediaType && !isClosed()) {
            response.putHeader(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        }
        return this;
    }

    @Override
    public HttpResponse header(Header header) {
        if (null != header && !isClosed()) {
            response.putHeader(header.getName(), Arrays.<String>asList(header.getValue()));
        }
        return this;
    }

    @Override
    public HttpResponse header(String header, String value) {
        if (header != null && value != null && !isClosed()) {
            response.putHeader(header, value);
        }
        return this;
    }

    @Override
    public Mono<Void> write(ByteBuf buffer) {
        if (isClosed()) {
            return Mono.empty();
        }
        return Mono.<Void>create(sink -> {
            Buffer buf = Buffer.buffer(buffer);
            netMonitor.bytesSent(buf.length());
            setResponseDefaultLength(buf.length());
            response.write(buf, v -> sink.success());
        }).doOnError(netMonitor::error);
    }

    @Override
    public Mono<Void> end() {
        if (isClosed()) {
            return Mono.empty();
        }
        return Mono.<Void>create(sink -> {
            if (response.ended()) {
                sink.success();
                return;
            }
            response.end(v -> sink.success());
        }).doOnError(netMonitor::error);
    }

    @Override
    public String getUrl() {
        return httpServerRequest.path();
    }

    @Override
    public String getPath() {
        return httpServerRequest.path();
    }

    @Override
    public String getRemoteIp() {
        return httpServerRequest.remoteAddress().host();
    }

    @Override
    public String getRealIp() {
        return VertxWebUtils.getIpAddr(httpServerRequest);
    }

    @Override
    public InetSocketAddress getClientAddress() {
        SocketAddress address = httpServerRequest.remoteAddress();
        if (null == address) {
            return null;
        }
        return new InetSocketAddress(getRealIp(), address.port());
    }

    @Override
    public MediaType getContentType() {
        String contentType = httpServerRequest.getHeader(HttpHeaders.CONTENT_TYPE);
        if (StringUtils.hasText(contentType)) {
            return MediaType.parseMediaType(contentType);
        } else {
            return MediaType.APPLICATION_FORM_URLENCODED;
        }
    }

    @Override
    public Optional<String> getQueryParameter(String key) {
        return Optional.ofNullable(httpServerRequest.getParam(key));
    }

    @Override
    public Map<String, String> getQueryParameters() {
        Map<String, String> params = new HashMap<>();

        MultiMap map = httpServerRequest.params();

        for (String name : map.names()) {
            params.put(name, String.join(",", map.getAll(name)));
        }

        return params;
    }

    @Override
    public Map<String, String> getRequestParam() {
        return convertRequestParam(httpServerRequest.formAttributes());
    }

    @Override
    public Mono<ByteBuf> getBody() {
        return body;
    }

    @Override
    public org.springframework.http.HttpMethod getMethod() {
        return convertMethodType(httpServerRequest.method());
    }

    @Override
    public List<Header> getHeaders() {
        return convertHeader(httpServerRequest.headers());
    }

    @Override
    public Optional<Header> getHeader(String key) {
        return Optional.ofNullable(
            getHeaders()
                .stream()
                .collect(Collectors.toMap(Header::getName, Function.identity()))
                .get(key));
    }

    @Override
    public Mono<HttpRequestMessage> toMessage() {
        return this.getBody()
                   .defaultIfEmpty(Unpooled.EMPTY_BUFFER)
                   .map(byteBuf -> {
                       DefaultHttpRequestMessage message = new DefaultHttpRequestMessage();
                       message.setContentType(this.getContentType());
                       message.setHeaders(this.getHeaders());
                       message.setMethod(this.getMethod());
                       message.setPayload(byteBuf);
                       message.setQueryParameters(this.getQueryParameters());
                       message.setUrl(this.getUrl());
                       return message;
                   });
    }
}
