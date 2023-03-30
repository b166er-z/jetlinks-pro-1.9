package org.jetlinks.pro.network.tcp.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import org.jetlinks.pro.network.tcp.TcpMessage;
import org.jetlinks.pro.network.tcp.parser.PayloadParser;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class VertxTcpClient implements TcpClient {

    public volatile NetClient client;

    public NetSocket socket;

    volatile PayloadParser payloadParser;

    public NetMonitor monitor;

    @Getter
    private final String id;

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();

    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();

    private final EmitterProcessor<TcpMessage> processor = EmitterProcessor.create(false);

    private final FluxSink<TcpMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final boolean serverClient;

    @Override
    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeoutMs = timeout.toMillis();
    }

    @Override
    public void reset() {
        if (null != payloadParser) {
            payloadParser.reset();
        }
    }

    @Override
    public boolean isAlive() {
        return socket != null && (keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs);
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    public VertxTcpClient(String id) {
        this.id = id;
        this.monitor = NetMonitors.getMonitor("tcp-client", "id", id);
        this.serverClient=false;
    }

    public VertxTcpClient(String id, NetMonitor monitor) {
        this.id = id;
        this.monitor = monitor;
        this.serverClient=true;
    }

    protected void received(TcpMessage message) {
        sink.next(message);
    }

    @Override
    public Flux<TcpMessage> subscribe() {
        return processor;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp client error", e);
            monitor.error(e);
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (null == socket) {
            return null;
        }
        SocketAddress socketAddress = socket.remoteAddress();
        return new InetSocketAddress(socketAddress.host(), socketAddress.port());
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_CLIENT;
    }

    @Override
    public void shutdown() {
        if (socket == null) {
            return;
        }
        log.debug("tcp client [{}] disconnect", getId());
        synchronized (this) {
            if (null != client) {
                execute(client::close);
                client = null;
            }
            if (null != socket) {
                execute(socket::close);
                this.socket = null;
            }
            if (null != payloadParser) {
                execute(payloadParser::close);
                payloadParser = null;
            }
        }
        for (Runnable runnable : disconnectListener) {
            execute(runnable);
        }
        disconnectListener.clear();
        if(serverClient){
            processor.onComplete();
        }
    }

    public void setClient(NetClient client) {
        if (this.client != null && this.client != client) {
            this.client.close();
        }
        keepAlive();
        this.client = client;
    }

    public void setRecordParser(PayloadParser payloadParser) {
        synchronized (this) {
            if (null != this.payloadParser && this.payloadParser != payloadParser) {
                this.payloadParser.close();
            }
            this.payloadParser = payloadParser;
            this.payloadParser
                .handlePayload()
                .onErrorContinue((err, res) -> monitor.error(err))
                .subscribe(buffer -> received(new TcpMessage(buffer.getByteBuf())));
        }
    }

    public void setSocket(NetSocket socket) {
        synchronized (this) {
            Objects.requireNonNull(payloadParser);
            if (this.socket != null && this.socket != socket) {
                this.socket.close();
            }
            this.socket = socket
                .closeHandler(v -> shutdown())
                .handler(buffer -> {
                    if (log.isDebugEnabled()) {
                        log.debug("handle tcp client[{}] payload:[{}]",
                                  socket.remoteAddress(),
                                  Hex.encodeHexString(buffer.getBytes()));
                    }
                    keepAlive();
                    monitor.bytesRead(buffer.length());
                    payloadParser.handle(buffer);
                    if (this.socket != socket) {
                        log.warn("tcp client [{}] memory leak ", socket.remoteAddress());
                        socket.close();
                    }
                });
        }
    }

    @Override
    public Mono<Boolean> send(TcpMessage message) {
        return Mono.<Boolean>create((sink) -> {
            if (socket == null) {
                sink.error(new SocketException("socket closed"));
                return;
            }
            Buffer buffer = Buffer.buffer(message.getPayload());
            int len = buffer.length();
            socket.write(buffer, r -> {
                keepAlive();
                if (r.succeeded()) {
                    monitor.bytesSent(len);
                    sink.success(true);
                } else {
                    sink.error(r.cause());
                }
            });
        }).doOnError(monitor::sendError);
    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }
}
