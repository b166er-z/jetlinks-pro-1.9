package org.jetlinks.pro.network.coap.gateway.device;

import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * CoAP接入设备会话
 *
 * @author zhouhao
 * @since 1.0
 */
class CoAPDeviceSession implements DeviceSession {

    private final DeviceOperator operator;

    //默认会话超时时间
    static Duration defaultKeepAliveTimeout = Duration.ofMinutes(31);

    @Setter
    private InetSocketAddress address;

    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    private long keepAliveTimeOutMs = defaultKeepAliveTimeout.toMillis();

    public CoAPDeviceSession(DeviceOperator operator, InetSocketAddress address, Transport transport) {
        this.operator = operator;
        this.address = address;
        this.transport = transport;
    }

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return operator;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
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
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeOutMs <= 0 || System.currentTimeMillis() - lastPingTime < keepAliveTimeOutMs;
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMs = timeout.toMillis();
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.of(address);
    }
}
