package org.jetlinks.pro.network.udp.gateway;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.pro.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.pro.network.udp.UdpMessage;
import org.jetlinks.pro.network.udp.UdpSupport;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

public class UdpDeviceSession implements DeviceSession {

    @Getter
    private final DeviceOperator operator;

    private UdpSupport support;

    @Setter
    private InetSocketAddress address;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    private long keepAliveTimeOutMS = Duration.ofMinutes(10).toMillis();

    private final DeviceGatewayMonitor monitor;

    @Getter
    private final Transport transport;

    public UdpDeviceSession(DeviceOperator operator,
                            UdpSupport support,
                            InetSocketAddress address,
                            Transport transport,
                            DeviceGatewayMonitor monitor) {
        this.operator = operator;
        this.support = support;
        this.address = address;
        this.transport = transport;
        this.monitor = monitor;
    }

    public void setSupport(UdpSupport support) {
        this.support = support;
      //  this.address = support.getBindAddress();
    }

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
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
        ping();
        return support
            .publish(new UdpMessage(encodedMessage.getPayload(), address))
            .doOnSuccess(v -> monitor.sentMessage())
            .thenReturn(true);
    }

    @Override
    public void close() {
        monitor.disconnected();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeOutMS < 0 || System.currentTimeMillis() - lastPingTime < keepAliveTimeOutMS;
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMS = timeout.toMillis();
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.of(address);
    }
}
