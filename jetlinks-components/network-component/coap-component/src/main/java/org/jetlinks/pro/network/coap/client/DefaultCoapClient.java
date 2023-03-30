package org.jetlinks.pro.network.coap.client;

import lombok.AllArgsConstructor;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.stack.ReliabilityLayerParameters;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.core.message.codec.CoapResponseMessage;
import org.jetlinks.core.message.codec.DefaultCoapResponseMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.monitor.NetMonitor;
import org.jetlinks.pro.network.monitor.NetMonitors;
import reactor.core.publisher.Mono;

import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 默认CoAP Client，使用https://github.com/eclipse/californium 实现CoAP客户端功能
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
public class DefaultCoapClient implements CoapClient {
    volatile org.eclipse.californium.core.CoapClient client;

    volatile CoapClientProperties properties;

    volatile NetMonitor monitor;

    public DefaultCoapClient(org.eclipse.californium.core.CoapClient coapClient, CoapClientProperties properties) {
        this(coapClient, properties, NetMonitors.getMonitor("coap-client", "id", properties.getId()));
    }

    @Override
    public CoapClientProperties getProperties() {
        return properties;
    }

    @Override
    public Mono<CoapResponseMessage> publish(CoapMessage message) {
        return Mono.defer(() -> publish(message.createRequest())
            .map(CoapResponseMessage::fromResponse));
    }

    @Override
    public Mono<CoapResponse> publish(Request request) {
        if (!isAlive()) {
            return Mono.error(() -> new SocketException("CoAP客户端已断开"));
        }
        if (request.getReliabilityLayerParameters() == null) {
            request.setReliabilityLayerParameters(ReliabilityLayerParameters
                                                      .builder()
                                                      .applyConfig(NetworkConfig.getStandard())
                                                      .maxRetransmit(properties.getRetryTimes())
                                                      .ackTimeout((int) properties.getTimeout())
                                                      .ackTimeoutScale(2F)
                                                      .build());
        }
        return Mono
            .<CoapResponse>create(sink -> {
                request.addMessageObserver(new MessageObserverAdapter() {
                    @Override
                    public void onReject() {
                        sink.error(new RejectedExecutionException("拒绝发送CoAP消息"));
                    }

                    @Override
                    public void onTimeout() {
                        sink.error(new TimeoutException("发送CoAP消息超时"));
                    }

                    @Override
                    public void onSent(boolean retransmission) {
                        monitor.bytesSent(request.getBytes().length);
                    }

                    @Override
                    public void onSendError(Throwable error) {
                        sink.error(error);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
                client.advanced(new CoapHandler() {

                    @Override
                    public void onLoad(CoapResponse response) {
                        sink.success(response);
                    }

                    @Override
                    public void onError() {

                    }
                }, request);
            })
            .timeout(Duration.ofMillis(properties.getTimeout() + 1000), Mono.error(TimeoutException::new))
            .doOnError(err -> monitor.error(err))
            .doOnNext(resp -> monitor.bytesRead(resp.getPayload() == null ? 0 : resp.getPayload().length));
    }

    @Override
    public String getId() {
        return properties.getId();
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_CLIENT;
    }

    @Override
    public void shutdown() {
        if (null != client) {
            client.getEndpoint().destroy();
        }
    }

    @Override
    public boolean isAlive() {
        return client != null && client.getEndpoint().isStarted();
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
