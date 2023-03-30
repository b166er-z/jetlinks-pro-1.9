package org.jetlinks.pro.network.websocket.executor;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.network.PubSubType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.Assert;

@Getter
@Setter
public class WebSocketClientTaskConfiguration {

    private String clientId;

    private PubSubType type;

    //发送数据类型
    private PayloadType sendPayloadType;

    //订阅数据类型
    private PayloadType subPayloadType;

    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty!");
        Assert.notNull(type, "type can not be null!");

    }
}
