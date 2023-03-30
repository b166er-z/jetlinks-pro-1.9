package org.jetlinks.pro.standalone.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.supports.cluster.event.RSocketAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Map;

@ConfigurationProperties(prefix = "jetlinks")
@Getter
@Setter
public class JetLinksProperties {

    private String serverId;

    private String clusterName = "default";

    private Map<String, Long> transportLimit;

    private EventBusProperties eventBus = new EventBusProperties();

    @Getter
    @Setter
    public static class EventBusProperties {

        RSocketEventBusProperties rsocket = new RSocketEventBusProperties();

    }

    @Getter
    @Setter
    public static class RSocketEventBusProperties {

        private boolean enabled = false;

        private RSocketAddress address = new RSocketAddress();

    }

    @PostConstruct
    @SneakyThrows
    public void init() {
        if (serverId == null) {
            serverId = InetAddress.getLocalHost().getHostName();
        }
    }
}
