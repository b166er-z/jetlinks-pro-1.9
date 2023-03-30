
package org.jetlinks.pro.network.udp.executor;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.network.PubSubType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.Assert;

@Getter
@Setter
public class UdpNodeConfiguration {

    private String clientId;

    private PubSubType type;

    private PayloadType sendPayloadType;

    private PayloadType subPayloadType;

    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty!");
        Assert.notNull(type, "type can not be null!");

    }
}
