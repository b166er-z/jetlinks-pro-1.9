package org.jetlinks.pro.network.websocket.gateway.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.pro.network.websocket.server.WebSocketServerClient;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * WebSocket设备会话
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
class WebSocketDeviceSession implements DeviceSession {

    @Getter
    @Setter
    private volatile DeviceOperator operator;

    private final InetSocketAddress address;

    private final WebSocketServerClient serverClient;

    @Getter
    private final Transport transport;

    private final long connectTime = System.currentTimeMillis();

    @Getter
    private Duration keepaliveTimeout;

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator == null ? "unknown" : operator.getDeviceId();
    }

    @Override
    public long lastPingTime() {
        return serverClient.getLastKeepAliveTime();
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return serverClient
            .send(((WebSocketMessage) encodedMessage))
            .thenReturn(true);
    }


    @Override
    public void close() {
        serverClient.close()
                    .subscribe();
    }

    @Override
    public void ping() {
    }

    @Override
    public boolean isAlive() {
        return serverClient.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        serverClient.closeHandler(call);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepaliveTimeout = timeout;
        serverClient.setKeepAliveTimeout(timeout);
    }

    public WebSocketDeviceSession copy() {
        return new WebSocketDeviceSession(operator, address, serverClient, transport, keepaliveTimeout);
    }
}
