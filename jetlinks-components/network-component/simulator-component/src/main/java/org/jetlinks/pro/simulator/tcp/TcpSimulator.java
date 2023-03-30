package org.jetlinks.pro.simulator.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.pro.network.security.CertificateManager;
import org.jetlinks.pro.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.pro.simulator.core.AbstractSimulator;
import org.jetlinks.pro.simulator.core.AddressPool;
import org.jetlinks.pro.simulator.core.SimulatorConfig;
import org.jetlinks.pro.simulator.core.SimulatorListenerBuilder;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TcpSimulator extends AbstractSimulator {

    private final Vertx vertx;

    private final CertificateManager keyManager;

    private final String type;

    TcpSimulator(Vertx vertx,
                 CertificateManager keyManager,
                 SimulatorConfig config,
                 SimulatorListenerBuilder builder,
                 AddressPool pool) {
        super(config, builder, pool);
        this.vertx = vertx;
        this.keyManager = keyManager;
        this.type = config.getType();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    protected Mono<TcpSession> createSession(int index, String bind) {

        Values networkConfig = Values.of(config.getNetwork().getConfiguration());

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("index", index);

        String certId = networkConfig.getValue("certId").map(Value::asString).orElse(null);
        boolean tls = networkConfig.getValue("tls").map(Value::asBoolean).orElse(false);

        String host = networkConfig
            .getValue("host")
            .map(Value::asString)
            .orElse("127.0.0.1");

        int port = networkConfig
            .getValue("port")
            .map(Value::asInt)
            .orElseThrow(() -> new IllegalArgumentException("port can not be null"));

        NetClientOptions clientOptions = new NetClientOptions();
        clientOptions.setLocalAddress(bind);

        TcpSession session = new TcpSession("tcp-" + index, index);

        session.setOptions(clientOptions);

        Function<NetClientOptions, Mono<NetClient>> clientBuilder;

        if (tls && StringUtils.hasText(certId)) {
            clientBuilder = opts -> keyManager
                .getCertificate(certId)
                .map(options -> {
                    VertxKeyCertTrustOptions keyOpts = new VertxKeyCertTrustOptions(options);
                    opts.setTrustOptions(keyOpts);
                    opts.setKeyCertOptions(keyOpts);
                    return vertx.createNetClient(opts);
                });
        } else {
            clientBuilder = opts -> Mono.just(vertx.createNetClient(opts));
        }

        session.setConnector(Mono.defer(() -> clientBuilder
            .apply(session.getOptions())
            .flatMap(client -> Mono
                .create(sink -> {
                    try {
                        client.connect(port, host, result -> {
                            if (result.succeeded()) {
                                sink.success(result.result());
                            } else {
                                sink.error(result.cause());
                            }
                        });
                    } catch (Throwable e) {
                        sink.error(e);
                    }
                })))
        );

        return Mono.just(session);
    }


}
