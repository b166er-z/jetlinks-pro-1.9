package org.jetlinks.pro.network.coap.gateway.device;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 未知设备会话,通过CoAP服务接入设备时，在协议解析上下文{@link FromDeviceMessageContext#getSession()}中,只能获取到此实例,
 * 因为平台在收到请求时，是无法识别出请求对应的设备的。因此在协议解析时,需要使用{@link FromDeviceMessageContext#getDevice(String)}来
 * 获取对应的设备
 *
 * @author zhouhao
 * @since 1.0
 */
class UnknownCoAPDeviceSession implements DeviceSession {

    private final Transport transport;

    private final InetSocketAddress socketAddress;

    private final Consumer<Duration> keepaliveTimeoutConsumer;

    public UnknownCoAPDeviceSession(Transport transport, InetSocketAddress address, Consumer<Duration> keepaliveTimeoutConsumer) {
        this.transport = transport;
        this.socketAddress = address;
        this.keepaliveTimeoutConsumer = keepaliveTimeoutConsumer;
    }

    @Override
    public String getId() {
        return "unknown";
    }

    @Override
    public String getDeviceId() {
        return "unknown";
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return 0;
    }

    @Override
    public long connectTime() {
        return 0;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.just(false);
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {

    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.of(socketAddress);
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepaliveTimeoutConsumer.accept(timeout);
    }
}
