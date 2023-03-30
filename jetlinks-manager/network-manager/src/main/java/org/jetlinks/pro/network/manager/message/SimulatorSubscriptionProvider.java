package org.jetlinks.pro.network.manager.message;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.simulator.core.SimulatorManager;
import org.jetlinks.pro.simulator.mqtt.MqttSession;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

@Component
@AllArgsConstructor
public class SimulatorSubscriptionProvider implements SubscriptionProvider {

    private final SimulatorManager simulatorManager;

    @Override
    public String id() {
        return "network-simulator";
    }

    @Override
    public String name() {
        return "模拟器消息订阅";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/simulator/**",
        };
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String topic = request.getTopic();
        Map<String, String> vars = TopicUtils.getPathVariables("/network/simulator/{id}/{sessionId}", topic);

        return simulatorManager
            .getSimulator(vars.get("id"))
            .flatMap(simulator -> simulator.getSession(vars.get("sessionId")))
            .flatMapMany(session -> {
                    if (session instanceof MqttSession) {
                        return ((MqttSession) session)
                            .subscribe(request.getString("topic", "/#"), request.getInt("qos", 0))
                            .map(MqttMessage::print);
                    }
                    return Flux
                        .create(sink -> sink
                            .onDispose(session.onDownstream(message -> sink.next(message.toString()))));
                }
            );
    }


}
