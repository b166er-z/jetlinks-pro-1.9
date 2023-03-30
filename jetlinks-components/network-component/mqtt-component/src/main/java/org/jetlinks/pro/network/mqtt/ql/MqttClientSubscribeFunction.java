package org.jetlinks.pro.network.mqtt.ql;

import lombok.AllArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.TableFunction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.mqtt.client.MqttClient;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.FromFeature;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <pre>
 *     select
 *       topic,
 *       payload
 *     from mqtt.client.subscribe('networkId','json','topic')
 * </pre>
 *
 * @author zhouhao
 */
@Component
@AllArgsConstructor
public class MqttClientSubscribeFunction implements FromFeature {

    private final NetworkManager networkManager;

    private static final String ID = FeatureId.From.of("mqtt.client.subscribe").getId();

    @Override
    public Function<ReactorQLContext, Flux<ReactorQLRecord>> createFromMapper(FromItem fromItem, ReactorQLMetadata metadata) {

        TableFunction from = ((TableFunction) fromItem);

        net.sf.jsqlparser.expression.Function function = from.getFunction();

        ExpressionList list = function.getParameters();
        if (list == null || CollectionUtils.isEmpty(list.getExpressions()) || list.getExpressions().size() < 3) {
            throw new IllegalArgumentException("参数错误:" + function);
        }

        String networkId = null;
        PayloadType messageType = null;
        List<String> topics = new ArrayList<>();
        for (int i = 0; i < list.getExpressions().size(); i++) {
            Expression expr = list.getExpressions().get(i);
            if (!(expr instanceof StringValue)) {
                throw new IllegalArgumentException("请传入字符参数:" + expr);
            }
            if (i == 0) {
                networkId = ((StringValue) expr).getValue();
            } else if (i == 1) {
                messageType = PayloadType.valueOf(((StringValue) expr).getValue().toUpperCase());
            } else {
                topics.add(((StringValue) expr).getValue());
            }
        }

        String fi = networkId;
        PayloadType type = messageType;
        String alias = from.getAlias() != null ? from.getAlias().getName() : null;
        return ctx -> networkManager
            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, fi)
            .flatMapMany(network -> network.subscribe(topics))
            .map(msg -> convertRecord(alias, ctx, msg, type))
            ;
    }

    private ReactorQLRecord convertRecord(String alias, ReactorQLContext ctx, MqttMessage msg, PayloadType type) {
        Map<String, Object> message = FastBeanCopier.copy(msg, new HashMap<>());
        message.put("payload", type.read(msg.getPayload()));
        message.put("payloadType", type.name());
        return ReactorQLRecord.newRecord(alias, message, ctx);
    }

    @Override
    public String getId() {
        return ID;
    }
}
