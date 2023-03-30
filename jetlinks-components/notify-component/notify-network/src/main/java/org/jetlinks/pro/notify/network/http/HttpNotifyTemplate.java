package org.jetlinks.pro.notify.network.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.core.Values;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.SimpleHttpRequestMessage;
import org.jetlinks.pro.notify.template.Template;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpNotifyTemplate implements Template {

    private String text;

    public HttpRequestMessage createMessage(Values values){

        return SimpleHttpRequestMessage.of(ExpressionUtils.analytical(text,values.getAllValues(),"spel"));

    }

    public static void main(String[] args) {
        String tmp="POST http://192.168.10.104:32000/api/v1/dataUpload/real/default\n" +
            "Content-Type: application/json\n" +
            "Authorization: Basic JGVmYjdiMzdhMjA2MDQwMjE5MmY5OGMyYTQ0ZjMxNGViOjRkMWEzNTA1NTU5NzQ1ZTJhZjk1NjcxZWQ2MTFjMTk3\n" +
            "\n" +
            "${T(com.alibaba.fastjson.JSON).toJSONString(#this)}";

        System.out.println(SimpleHttpRequestMessage.of(tmp).print());
    }

}
