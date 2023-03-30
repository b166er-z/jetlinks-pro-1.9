package org.jetlinks.pro.network.http.server.vertx;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.topic.Topic;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.http.server.HttpExchange;
import org.jetlinks.pro.network.http.server.HttpServer;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import org.springframework.http.HttpStatus;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class VertxHttpServer implements HttpServer {

    private Collection<io.vertx.core.http.HttpServer> httpServers;

    private HttpServerConfig config;

    private String id;

    public NetMonitor netMonitor;

    private EmitterProcessor<HttpExchange> httpServerProcessor = EmitterProcessor.create(false);

    private FluxSink<HttpExchange> exchangeFluxSink = httpServerProcessor.sink();

    private final Topic<FluxSink<HttpExchange>> route = Topic.createRoot();

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    public VertxHttpServer(HttpServerConfig config) {
        this.config = config;
        this.id = config.getId();
        netMonitor = NetMonitors.getMonitor("http-server", "id", id);
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    public void setHttpServers(Collection<io.vertx.core.http.HttpServer> httpServers) {
        if (isAlive()) {
            shutdown();
        }
        this.httpServers = httpServers;
        for (io.vertx.core.http.HttpServer server : this.httpServers) {
            server.requestHandler(request -> {
                request.exceptionHandler(err -> {
                    netMonitor.error(err);
                    log.error(err.getMessage(), err);
                });
                netMonitor.bytesRead(request.bytesRead());

                VertxHttpExchange exchange = new VertxHttpExchange(request, config, netMonitor);

                String url = exchange.getUrl();
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }

                route.findTopic("/" + exchange.request().getMethod().name().toLowerCase() + url)
                    .flatMapIterable(Topic::getSubscribers)
                    .doOnNext(sink -> sink.next(exchange))
                    .switchIfEmpty(Mono.fromRunnable(() -> {
                        if (!httpServerProcessor.hasDownstreams()) {
                            log.warn("http server no handler for:[{} {}://{}{}]", request.method(), request.scheme(), request.host(), request.path());
                            request.response()
                                .setStatusCode(HttpStatus.NOT_FOUND.value())
                                .end();
                        }
                    }))
                    .subscribe();

                if (httpServerProcessor.hasDownstreams()) {
                    exchangeFluxSink.next(exchange);
                }
            });
        }
    }

    @Override
    public Flux<HttpExchange> handleRequest() {
        return httpServerProcessor;
    }


    @Override
    public Flux<HttpExchange> handleRequest(String method, String... urlPatterns) {
        return Flux.create(sink -> {
            Disposable.Composite disposable = Disposables.composite();
            for (String urlPattern : urlPatterns) {
                String pattern = Stream.of(urlPattern.split("[/]"))
                    .map(str -> {
                        //处理路径变量,如: /devices/{id}
                        if (str.startsWith("{") && str.endsWith("}")) {
                            return "*";
                        }
                        return str;
                    })
                    .collect(Collectors.joining("/"));
                if (pattern.endsWith("/")) {
                    pattern = pattern.substring(0, pattern.length() - 1);
                }
                if (!pattern.startsWith("/")) {
                    pattern = "/".concat(pattern);
                }
                pattern = "/" + method + pattern;
                log.debug("handle http request : {}", pattern);
                Topic<FluxSink<HttpExchange>> sub = route.append(pattern);
                sub.subscribe(sink);
                disposable.add(() -> sub.unsubscribe(sink));
            }
            sink.onDispose(disposable);
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public void shutdown() {
        if (httpServers != null) {
            for (io.vertx.core.http.HttpServer httpServer : httpServers) {
                httpServer.close(res -> {
                    if (res.failed()) {
                        log.error(res.cause().getMessage(), res.cause());
                    } else {
                        log.debug("http server [{}] closed", httpServer.actualPort());
                    }
                });
            }
            httpServers.clear();
            httpServers = null;
        }
    }

    @Override
    public boolean isAlive() {
        return httpServers != null && !httpServers.isEmpty();
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
