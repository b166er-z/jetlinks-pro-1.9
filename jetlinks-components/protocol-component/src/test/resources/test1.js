codec.decoder(function (context){ //解码函数
    var message = context.getMessage(); //获取消息体，json格式
    var topic = message.getUrl().toString();

    var payload = message.payloadAsJson();
    if(topic.indexOf("10045219/device-change")>-1) { //"/10045219/device-change" 设备数据变化通知
        if (payload.deviceId !=null && payload.messageType === "dataReport") {


            var msg = {};
            msg.deviceId = payload.deviceId;
            msg.messageId = Packages.org.hswebframework.web.id.IDGenerator.SNOW_FLAKE_STRING.generate();
            msg.timestamp = payload.timestamp;
            msg.properties = payload.payload;
            msg.messageType = "REPORT_PROPERTY";
            return message.ok( JSON.stringify({'success': true})).thenReturn(msg);
        }
    }
    if(topic.indexOf("10045219/device-cmdresponse")>-1) {//"/10045219/device-cmdresponse" 设备指令响应通知
            return reactor.core.publisher.Mono.empty();
    }
    if(topic.indexOf("10045219/device-event")>-1) {//"/10045219/device-event" 设备事件上报通知

        if (payload.deviceId !=null && payload.messageType === "dataReport") {


            var msg = {};
            msg.deviceId = payload.deviceId;
            msg.messageId = Packages.org.hswebframework.web.id.IDGenerator.SNOW_FLAKE_STRING.generate();
            msg.timestamp = payload.timestamp;
            msg.properties = payload.payload;
            msg.messageType = "REPORT_PROPERTY";
            return message.ok( JSON.stringify({'success': true})).thenReturn(msg);
        }
    }
    if(topic.indexOf("10045219/device-offonline")>-1) {//"/10045219/device-offonline" 设备上下线通知
        if (payload.deviceId != null && payload.messageType === "deviceOnlineOfflineReport") {

            var msg = {};  //上发报文
            if (payload.eventType === 1) {
                msg.messageType = "ONLINE";//参考org.jetlinks.core.message.MessageType,注意msg的成员和类型的对应关系
            } else {
                msg.messageType = "OFFLINE";
            }
            msg.deviceId = payload.deviceId;
            return message.ok( JSON.stringify({'success': true})).thenReturn(msg);
        } else {
            return message.error(405,topic.indexOf("/10045219/device-change") +"|" +topic + "|" +payload.messageType);
        }
    }






});
codec.encoder("READ_PROPERTY",function(context){ //编码函数，用于反控，如果没有反控需要保持空函数，不能删除
});