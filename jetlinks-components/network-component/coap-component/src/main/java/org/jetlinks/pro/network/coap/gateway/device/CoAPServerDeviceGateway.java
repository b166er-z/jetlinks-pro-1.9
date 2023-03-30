package org.jetlinks.pro.network.coap.gateway.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.pro.gateway.monitor.GatewayMonitors;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.coap.server.CoapServer;
import org.jetlinks.pro.network.utils.DeviceGatewayHelper;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * CoAP 服务设备接入网关,处理网络组建中的CoAP Server的请求,使用指定的协议包来解析这些请求为设备消息。
 *
 * @author zhouhao
 * @see CoapExchangeMessage
 * @see CoapServer
 * @see DefaultNetworkType#COAP_SERVER
 * @since 1.0
 */
@Slf4j
public class CoAPServerDeviceGateway implements DeviceGateway {

    @Getter
    private final String id;

    private final DeviceRegistry deviceRegistry;

    private final CoapServer coapServer;

    private final EmitterProcessor<Message> messageProcessor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final Supplier<Mono<ProtocolSupport>> supportSupplier;

    private final DeviceGatewayMonitor monitor;

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    public CoAPServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   CoapServer coapServer,
                                   Supplier<Mono<ProtocolSupport>> supportSupplier,
                                   DeviceSessionManager sessionManager,
                                   DecodedClientMessageHandler handler) {
        this.id = id;
        this.deviceRegistry = registry;
        this.coapServer = coapServer;
        this.supportSupplier = supportSupplier;
        this.monitor = GatewayMonitors.getDeviceGatewayMonitor(id, "network", "coap");
        this.helper = new DeviceGatewayHelper(registry, sessionManager, handler);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.CoAP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    AtomicBoolean started = new AtomicBoolean();

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = coapServer
            .subscribe()
            .filter(r -> started.get())
            .flatMap(exchange -> Mono.just(exchange).zipWith(supportSupplier.get()))
            .flatMap(msgAndProtocol -> {
                CoapExchange exchange = msgAndProtocol.getT1();
                ProtocolSupport protocolSupport = msgAndProtocol.getT2();
                InetAddress address = exchange.getSourceAddress();
                InetSocketAddress socketAddress = new InetSocketAddress(address.getHostAddress(), exchange.getSourcePort());
                CoapMessage coapMessage = new CoapExchangeMessage(exchange);
                if (log.isDebugEnabled()) {
                    log.debug("收到CoAP[{}]报文:\n{}", socketAddress, coapMessage);
                }
                AtomicReference<Duration> keepAliveTimeoutRef = new AtomicReference<>();
                monitor.receivedMessage();
                return protocolSupport
                    .getMessageCodec(getTransport())
                    //解码
                    .flatMapMany(codec -> codec
                        .decode(FromDeviceMessageContext.of(
                            new UnknownCoAPDeviceSession(getTransport(), socketAddress, keepAliveTimeoutRef::set),
                            coapMessage,
                            deviceRegistry)))
                    .cast(DeviceMessage.class)
                    .flatMap(deviceMessage -> {
                        if (messageProcessor.hasDownstreams()) {
                            sink.next(deviceMessage);
                        }
                        //CoAP强制设置保持在线
                        deviceMessage.addHeaderIfAbsent(Headers.keepOnline, true);
                        return helper
                            .handleDeviceMessage(deviceMessage,
                                                 //创建session
                                                 device -> new CoAPDeviceSession(device, socketAddress, getTransport()),
                                                 //设置超时时间
                                                 DeviceGatewayHelper.applySessionKeepaliveTimeout(deviceMessage, keepAliveTimeoutRef::get),
                                                 () -> log.warn("无法从CoAP消息中获取设备信息:\n{}\n\n设备消息:{}", coapMessage, deviceMessage))
                            .then();
                    })
                    .doOnEach(ReactiveLogger.onError(err -> log.error("处理 CoAP[{}] 消息失败\n{}",
                                                                      socketAddress,
                                                                      coapMessage,
                                                                      err)))
                    .onErrorResume(err -> Mono.empty());
            })
            .onErrorResume(err -> {
                log.error("handle CoAP message error", err);
                return Mono.empty();
            })
            .subscriberContext(ReactiveLogger.start("network", coapServer.getId()))
            .subscribe();

    }


    @Override
    public Flux<Message> onMessage() {
        return messageProcessor;
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
