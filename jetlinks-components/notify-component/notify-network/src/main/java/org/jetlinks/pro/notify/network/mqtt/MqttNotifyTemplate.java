package org.jetlinks.pro.notify.network.mqtt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.core.Values;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.pro.notify.template.Template;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MqttNotifyTemplate implements Template {

    private String text;

    public MqttMessage createMessage(Values values){

        return SimpleMqttMessage.of(ExpressionUtils.analytical(text,values.getAllValues(),"spel"));

    }

}
