package org.jetlinks.pro.network.http.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.pro.gateway.monitor.GatewayMonitors;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.http.server.HttpExchange;
import org.jetlinks.pro.network.http.server.HttpServer;
import org.jetlinks.pro.network.utils.DeviceGatewayHelper;
import org.jetlinks.pro.utils.SystemUtils;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.http.HttpStatus;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Http 服务设备网关，使用指定的协议包，将网络组件中Http服务的请求处理为设备消息
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class HttpServerDeviceGateway implements DeviceGateway {

    @Getter
    private final String id;

    private final HttpServer httpServer;

    private final Function<String, Mono<ProtocolSupport>> protocolSupportSupplier;

    private final DeviceRegistry registry;

    private final DeviceGatewayMonitor gatewayMonitor;

    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private Disposable disposable;

    private final AtomicReference<Boolean> started = new AtomicReference<>(false);

    private final String[] urls;

    private final DeviceGatewayHelper helper;

    public HttpServerDeviceGateway(String id,
                                   String[] urls,
                                   HttpServer server,
                                   Function<String, Mono<ProtocolSupport>> supplier,
                                   DeviceSessionManager sessionManager,
                                   DeviceRegistry registry,
                                   DecodedClientMessageHandler messageHandler) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.httpServer = server;
        this.protocolSupportSupplier = supplier;
        this.registry = registry;
        this.urls = urls;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
    }


    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public Flux<Message> onMessage() {
        return processor;
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = httpServer
            .handleRequest("*", urls)
            .filterWhen(exchange -> {
                if (!started.get()) {
                    return exchange
                        .error(HttpStatus.BAD_GATEWAY)
                        .thenReturn(false);
                }
                return Mono.just(true);
            })
            .flatMap(exchange -> protocolSupportSupplier
                .apply(exchange.request().getUrl())
                .flatMap(protocol -> exchange
                    .toExchangeMessage()
                    .flatMap(httpMessage -> {
                        if (log.isDebugEnabled()) {
                            log.debug("收到HTTP请求\n{}", httpMessage);
                        }
                        InetSocketAddress address = exchange.request().getClientAddress();
                        AtomicReference<Duration> timeoutRef = new AtomicReference<>(Duration.ofMinutes(30));

                        //调用协议执行解码
                        return protocol
                            .getMessageCodec(getTransport())
                            .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(new UnknownHttpDeviceSession() {
                                @Override
                                public void setKeepAliveTimeout(Duration timeout) {
                                    timeoutRef.set(timeout);
                                }

                                @Override
                                public Optional<InetSocketAddress> getClientAddress() {
                                    return Optional.of(address);
                                }
                            }, httpMessage, registry)))
                            .cast(DeviceMessage.class)
                            .flatMap(deviceMessage -> {
                                if (processor.hasDownstreams()) {
                                    sink.next(deviceMessage);
                                }
                                gatewayMonitor.receivedMessage();

                                return helper
                                    .handleDeviceMessage(deviceMessage,
                                                         device -> new HttpDeviceSession(device, address),
                                                         DeviceGatewayHelper.applySessionKeepaliveTimeout(deviceMessage, timeoutRef::get),
                                                         () -> {
                                                             log.warn("无法从HTTP消息中获取设备信息:\n{}\n\n设备消息:{}", httpMessage, deviceMessage);
                                                             return exchange
                                                                 .error(HttpStatus.NOT_FOUND)
                                                                 .then(Mono.empty());
                                                         });
                            })
                            .then(Mono.defer(() -> {
                                      //如果协议包里没有回复，那就响应200
                                      if (!exchange.isClosed()) {
                                          return exchange.ok();
                                      }
                                      return Mono.empty();
                                  })
                            )
                            .onErrorResume(err -> {
                                log.error("处理http请求失败:\n{}", httpMessage, err);
                                return response500Error(exchange, err);
                            })
                            .then();
                    })
                    .onErrorResume((error) -> {
                        log.error(error.getMessage(), error);
                        return response500Error(exchange, error);
                    })
                ), Integer.MAX_VALUE)
            .onErrorContinue((error, res) -> log.error(error.getMessage(), error))
            .subscribe();
    }

    private Mono<Void> response500Error(HttpExchange exchange, Throwable err) {
        return exchange.error(HttpStatus.INTERNAL_SERVER_ERROR, err);
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> started.set(false));
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            started.set(false);
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            disposable = null;
        });
    }

    @Override
    public boolean isAlive() {
        return started.get();
    }
}
