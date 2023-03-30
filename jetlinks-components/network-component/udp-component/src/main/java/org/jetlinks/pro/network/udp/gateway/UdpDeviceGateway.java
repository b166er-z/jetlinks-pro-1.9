package org.jetlinks.pro.network.udp.gateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.pro.gateway.monitor.GatewayMonitors;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.udp.UdpMessage;
import org.jetlinks.pro.network.udp.UdpSupport;
import org.jetlinks.pro.network.utils.DeviceGatewayHelper;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * UDP设备网关，使用指定的协议包，将网络组件中UDP支持的请求处理为设备消息
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class UdpDeviceGateway implements DeviceGateway {

    @Getter
    private final String id;

    private final Supplier<Mono<ProtocolSupport>> protocolSupplier;

    private final UdpSupport udpSupport;

    private final DeviceRegistry registry;

    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final DeviceGatewayMonitor monitor;

    private final DeviceGatewayHelper helper;

    public UdpDeviceGateway(String id,
                            Supplier<Mono<ProtocolSupport>> protocolSupplier,
                            DeviceSessionManager sessionManager,
                            UdpSupport udpSupport,
                            DecodedClientMessageHandler messageHandler,
                            DeviceRegistry registry) {

        this.id = id;
        this.protocolSupplier = protocolSupplier;
        this.udpSupport = udpSupport;
        this.registry = registry;
        this.monitor = GatewayMonitors.getDeviceGatewayMonitor(id, "network", "udp");
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.UDP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public Flux<Message> onMessage() {
        return processor;
    }

    private Disposable disposable;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = udpSupport
            .subscribe()
            .filter(msg -> started.get())
            .flatMap(msg -> Mono.just(msg)
                                .zipWith(protocolSupplier
                                             .get()
                                             .onErrorResume(err -> Mono.fromRunnable(() -> log.warn("获取协议失败", err)))))
            .flatMap(tp2 -> {
                ProtocolSupport protocol = tp2.getT2();
                UdpMessage message = tp2.getT1();
                log.debug("收到UDP[{}]报文:\n{}", message.getAddress(), message);

                AtomicReference<Duration> keepAliveTimeoutRef = new AtomicReference<>();

                return protocol
                    .getMessageCodec(getTransport())
                    .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                        @Override
                        public DeviceSession getSession() {
                            return new UnknownUdpDeviceSession(udpSupport, message.getAddress(), getTransport(), monitor) {
                                @Override
                                public void setKeepAliveTimeout(Duration timeout) {
                                    keepAliveTimeoutRef.set(timeout);
                                }
                            };
                        }

                        @Override
                        @Nonnull
                        public EncodedMessage getMessage() {
                            return message;
                        }

                        @Override
                        public Mono<DeviceOperator> getDevice(String deviceId) {
                            return registry.getDevice(deviceId);
                        }
                    }))
                    .doOnNext(msg -> monitor.receivedMessage())
                    .cast(DeviceMessage.class)
                    .flatMap(msg -> {
                        if (processor.hasDownstreams()) {
                            sink.next(msg);
                        }
                        return helper
                            .handleDeviceMessage(
                                msg,
                                device -> new UdpDeviceSession(device, udpSupport, message.getAddress(), getTransport(), monitor),
                                DeviceGatewayHelper
                                    .applySessionKeepaliveTimeout(msg, keepAliveTimeoutRef::get)
                                    .andThen(session -> {
                                        UdpDeviceSession deviceSession = session.unwrap(UdpDeviceSession.class);
                                        deviceSession.setAddress(message.getAddress());
                                        deviceSession.setSupport(udpSupport);
                                    }),
                                () -> log.warn("无法从udp请求[{}]消息中获取设备信息:{}", message.getAddress(), msg)
                            );
                    })
                    .onErrorResume((err) -> {
                        log.error("处理UDP[{}]消息失败", message, err);
                        return Mono.empty();
                    });
            })
            .onErrorContinue((err, v) -> log.error("处理UDP消息失败", err))
            .doFinally(s -> log.debug("upd device gateway closed "))
            .subscribe();

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
