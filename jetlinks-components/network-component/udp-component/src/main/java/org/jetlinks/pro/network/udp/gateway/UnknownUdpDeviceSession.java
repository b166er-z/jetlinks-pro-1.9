package org.jetlinks.pro.network.udp.gateway;

import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.pro.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.pro.network.udp.UdpMessage;
import org.jetlinks.pro.network.udp.UdpSupport;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

public class UnknownUdpDeviceSession implements DeviceSession {


    private UdpSupport support;

    private InetSocketAddress address;

    private long lastPingTime;

    private long connectTime = System.currentTimeMillis();

    private DeviceGatewayMonitor monitor;

    public UnknownUdpDeviceSession(UdpSupport support,
                                   InetSocketAddress address,
                                   Transport transport, DeviceGatewayMonitor monitor) {
        this.support = support;
        this.address = address;
        this.transport = transport;
        this.monitor=monitor;
    }

    @Getter
    private Transport transport;

    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public String getId() {
        return "udp-" + address.toString();
    }

    @Override
    public String getDeviceId() {
        return "known";
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
        return support
            .publish(new UdpMessage(encodedMessage.getPayload(), address))
            .doOnSuccess((r)->monitor.sentMessage())
            .thenReturn(true);
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
        return true;
    }

    @Override
    public void onClose(Runnable call) {

    }
}
