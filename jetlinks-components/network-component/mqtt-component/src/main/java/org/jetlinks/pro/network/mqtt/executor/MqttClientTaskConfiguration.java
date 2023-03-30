package org.jetlinks.pro.network.mqtt.executor;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.pro.network.PubSubType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class MqttClientTaskConfiguration {

    private String clientId;

    private PayloadType payloadType = PayloadType.JSON;

    private PubSubType[] clientType;

    private List<String> topics;

    private List<String> topicVariables;

    public List<String> getTopics(Map<String, Object> vars) {
        if (topics == null) {
            return Collections.emptyList();
        }
        return topics.stream()
            .map(topic -> ExpressionUtils.analytical(topic, vars, "spel")).collect(Collectors.toList());
    }

    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty");
        Assert.notNull(clientType, "clientType can not be null");
        Assert.notEmpty(topics, "topics can not be empty");

    }
}
