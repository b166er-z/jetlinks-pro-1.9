package org.jetlinks.pro.network.http.client;

import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.*;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.http.DefaultHttpResponseMessage;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 使用Vertx实现Http 客户端
 *
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@Getter
public class VertxHttpClient implements HttpClient {

    private volatile WebClient webClient;

    @Setter
    private volatile HttpClientConfig clientConfig;

    private final String id;

    public NetMonitor netMonitor;

    public VertxHttpClient(String id) {
        this.id = id;
        netMonitor = NetMonitors.getMonitor("http-client", "id", id);
    }

    public void setWebClient(WebClient webClient) {
        if (this.webClient != null && this.webClient != webClient) {
            shutdown();
        }
        this.webClient = webClient;
    }

    private final static Map<Request, Invoker> invokerSupports = new ConcurrentHashMap<>();

    {
        invokerSupports.put(Request.of(HttpMethod.GET, null), this::sendHandle);

        invokerSupports.put(Request.of(HttpMethod.DELETE, null), this::sendHandle);

        invokerSupports.put(Request.of(HttpMethod.POST, MediaType.APPLICATION_FORM_URLENCODED), this::sendBufferHandle);

        invokerSupports.put(Request.of(HttpMethod.PUT, MediaType.APPLICATION_FORM_URLENCODED), this::sendBufferHandle);

        invokerSupports.put(Request.of(HttpMethod.PATCH, MediaType.APPLICATION_FORM_URLENCODED), this::sendBufferHandle);

        invokerSupports.put(Request.of(HttpMethod.POST, MediaType.APPLICATION_JSON), this::sendBufferHandle);

        invokerSupports.put(Request.of(HttpMethod.PUT, MediaType.APPLICATION_JSON), this::sendBufferHandle);

        invokerSupports.put(Request.of(HttpMethod.PATCH, MediaType.APPLICATION_JSON), this::sendBufferHandle);
    }

    private void sendHandle(HttpRequestMessage message, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        commonRequest(message).send(handler);
    }

    private void sendBufferHandle(HttpRequestMessage message, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        HttpRequest<Buffer> request = commonRequest(message);
        Buffer buffer = Buffer.buffer(message.getPayload());
        netMonitor.bytesRead(buffer.length());
        request.sendBuffer(buffer, handler);
    }

    @SneakyThrows
    private HttpRequest<Buffer> commonRequest(HttpRequestMessage message) {
        String url = urlHandle(message.getUrl());

        HttpRequest<Buffer> request = webClient.requestAbs(
            convertMethodType(message.getMethod().toString()),
            url);

        clientConfig.getHttpHeaders().forEach(request::putHeader);
        Optional.ofNullable(message.getQueryParameters())
                .ifPresent(map -> map.forEach(request::addQueryParam));
        Optional.ofNullable(message.getContentType())
                .ifPresent(contentType -> request.putHeader("content-type", contentType.toString()));

        Optional.of(message.getHeaders())
                .ifPresent(headers -> headers.forEach(header -> request.putHeader(header.getName(), Arrays.asList(header
                                                                                                                      .getValue()))));

        return request;
    }

    private String urlHandle(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else {
            return clientConfig.getBaseUrl().concat(url);
        }
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Override
    public void shutdown() {
        try {
            if (webClient != null) {
                webClient.close();
            }
            webClient = null;
        } catch (Exception ignore) {

        }
    }

    @Override
    public boolean isAlive() {
        return webClient != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @EqualsAndHashCode
    @AllArgsConstructor(staticName = "of")
    static class Request {
        HttpMethod method;
        MediaType mediaType;
    }

    interface Invoker {
        void doRequest(HttpRequestMessage message, Handler<AsyncResult<HttpResponse<Buffer>>> handler);
    }

    private HttpMethod convertMethodType(String method) {
        return HttpMethod.valueOf(method.toUpperCase());
    }

    @Override
    public Flux<? extends HttpResponseMessage> request(HttpRequestMessage message) {
        return Flux.<HttpResponseMessage>create(sink -> {
            netMonitor.send();
            Invoker invoker = invokerSupports.get(Request.of(convertMethodType(message
                                                                                   .getMethod()
                                                                                   .toString()), message.getContentType()));
            if (invoker == null && message.getContentType() != null) {
                invoker = invokerSupports.get(Request.of(convertMethodType(message.getMethod().toString()), null));
            }
            if (invoker == null) {
                sink.error(new UnsupportedOperationException("unsupported http request: " + message
                    .getMethod()
                    .toString() + " " + message.getContentType()));
                return;
            }
            NetMonitor.Timer timer = netMonitor.timer();
            timer.start();
            invoker.doRequest(
                message,
                result -> {
                    try {
                        timer.end();
                        if (result.succeeded()) {
                            netMonitor.sendComplete();
                            HttpResponse<Buffer> response = result.result();
                            List<Header> headers = response
                                .headers()
                                .entries()
                                .stream()
                                .map(entry -> {
                                    Header header = new Header();
                                    header.setName(entry.getKey());
                                    header.setValue(new String[]{entry.getValue()});
                                    return header;
                                }).collect(Collectors.toList());
                            String contentType = response.getHeader("content-type");
                            Buffer body = response.body();
                            netMonitor.bytesRead(body == null ? 0 : body.length());
                            DefaultHttpResponseMessage responseMessage = new DefaultHttpResponseMessage();
                            responseMessage.setStatus(response.statusCode());
                            responseMessage.setPayload(body == null ? Unpooled.wrappedBuffer(new byte[0]) : body.getByteBuf());
                            responseMessage.setHeaders(headers);
                            if (!StringUtils.isEmpty(contentType)) {
                                responseMessage.setContentType(MediaType.parseMediaType(contentType));
                            }
                            sink.next(responseMessage);
                            sink.complete();
                        } else {
                            sink.error(result.cause());
                        }
                    } catch (Throwable e) {
                        sink.error(e);
                    }
                });
        })
            .doOnError(netMonitor::error);
    }
}
