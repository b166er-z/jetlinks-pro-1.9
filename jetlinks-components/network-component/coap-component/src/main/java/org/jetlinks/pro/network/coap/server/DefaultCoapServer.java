package org.jetlinks.pro.network.coap.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.monitor.NetMonitor;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.net.InetSocketAddress;
import java.util.function.Function;

/**
 * 默认的CoAP Server实现,使用 https://github.com/eclipse/californium 实现CoAP服务功能
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class DefaultCoapServer extends CoapResource implements CoapServer {

    private org.eclipse.californium.core.CoapServer coapServer;

    private final EmitterProcessor<CoapExchange> exchangeProcessor = EmitterProcessor.create(false);

    private final FluxSink<CoapExchange> sink = exchangeProcessor.sink();

    private final NetMonitor monitor;

    private final String id;

    @Getter
    @Setter
    private String lastError;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    public DefaultCoapServer(String id, NetMonitor monitor) {
        super("");
        this.monitor = monitor;
        this.id = id;
        setVisible(true);
    }

    public void setCoapServer(org.eclipse.californium.core.CoapServer coapServer) {
        if (this.coapServer != null) {
            try {
                shutdown();
            } catch (Exception ignore) {
            }
        }
        this.coapServer = coapServer;
        this.coapServer.start();
    }

    @Override
    public Flux<CoapExchange> subscribe() {
        return exchangeProcessor
            .doOnNext(exchange -> monitor.handled());
    }

    @Override
    public void handleRequest(Exchange exchange) {
        monitor.bytesRead(exchange.getRequest().getPayloadSize());
        if (exchangeProcessor.hasDownstreams()) {
            sink.next(new CoapExchange(exchange, this));
        } else {
            log.warn("coap server no handler");
            exchange.sendReject();
        }
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public void shutdown() {
        if (null != coapServer) {
            coapServer.destroy();
        }
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
