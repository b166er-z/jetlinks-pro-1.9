package org.jetlinks.pro.network.tcp.gateway.device;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.tcp.server.TcpServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Component
public class TcpServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;


    public TcpServerDeviceGatewayProvider(NetworkManager networkManager,
                                          DeviceRegistry registry,
                                          DeviceSessionManager sessionManager,
                                          DecodedClientMessageHandler messageHandler,
                                          ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "tcp-server-gateway";
    }

    @Override
    public String getName() {
        return "TCP 透传接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<TcpServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(mqttServer -> {
                String protocol = (String) properties.getConfiguration().get("protocol");

                Assert.hasText(protocol,"protocol can not be empty");

               return new TcpServerDeviceGateway(properties.getId(),
                    protocol,
                    protocolSupports,
                    registry,
                    messageHandler,
                    sessionManager,
                    mqttServer
                );
            });
    }
}
