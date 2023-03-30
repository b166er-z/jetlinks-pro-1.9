/**
 * 烟感设备模拟器
 */
var _logger = logger;
//设备实例id前缀
var devicePrefix = "test";

var eventId = Math.ceil(Math.random() * 1000);
//事件类型
var events = {
    reportProperty: function (index, session) {
        var deviceId = "test001";
        var topic = "/report-property";

        var prop = new java.util.HashMap();
        //prop.put("temperature", java.util.concurrent.ThreadLocalRandom.current().nextDouble(20, 30))
        for (var i = 0; i < 3; i++) {
            prop.put("temp" + i, randomFloat())
        }
        var json = com.alibaba.fastjson.JSON.toJSONString({
            "deviceId": deviceId,
            "success": true,
            "timestamp": new Date().getTime(),
            // "headers":{"keepOnline":true},
            properties: prop,
        });
        session.sendMessage(topic, json)
    },
    fireAlarm: function (index, session) {
        var deviceId = "test001";
        var topic = "/fire_alarm/department/1/area/1/dev/" + deviceId;
        var json = JSON.stringify({
            "deviceId": deviceId, // 设备编号 "pid": "TBS-110", // 设备编号
            "a_name": "商务大厦", // 区域名称 "bid": 2, // 建筑 ID
            "b_name": "C2 栋", // 建筑名称
            "l_name": "12-05-201", // 位置名称
            "time": "2020-04-20 13:12:11",
            "timestamp": new Date().getTime() // 消息时间
        });

        session.sendMessage(topic, json)
    }
};
var counter = 0;

function randomFloat(){
    var val=java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30);
    return val;
    // return parseInt(val*100)/100;
}

//事件上报
simulator.onEvent(function (index, session) {
    counter++;
    // if (counter >= 200000) {
    //     java.lang.System.exit(0);
    //     return;
    // }
    //上报属性
    events.reportProperty(index, session);
    // events.reportProperty(index, session);
    // events.reportProperty(index, session);
    // events.reportProperty(index, session);
    // events.reportProperty(index, session);

    //上报火警
   // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    //
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);
    // events.fireAlarm(index, session);

});

simulator.bindHandler("/read-property", function (message, session) {

    var prop = new java.util.HashMap();
    prop.put("temperature", java.util.concurrent.ThreadLocalRandom.current().nextDouble(20, 30))
    for (var i = 0; i < 40; i++) {
        prop.put("temp" + i, java.util.concurrent.ThreadLocalRandom.current().nextDouble(20, 30))

    }

    var reply = {
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: prop,
        success: true
    };

    _logger.info("读取属性:[{}]:{}", message, com.alibaba.fastjson.JSON.toJSONString(reply));
    session.sendMessage("/read-property-reply", com.alibaba.fastjson.JSON.toJSONString(reply));
    // simulator.runDelay(function () {
    //     session.sendMessage("/read-property-reply", JSON.stringify({
    //         messageId: message.messageId,
    //         deviceId: message.deviceId,
    //         timestamp: new Date().getTime(),
    //         properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30)},
    //         success: true
    //     }));
    // }, 2000)

});


simulator.bindHandler("/children/read-property", function (message, session) {
    _logger.info("读取子设备属性:[{}]", message);
    session.sendMessage("/children/read-property-reply", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30)},
        success: true
    }));
});

simulator.bindHandler("/children/read-property", function (message, session) {
    _logger.info("读取子设备属性:[{}]", message);
    session.sendMessage("/children/read-property-reply", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30)},
        success: true
    }));
});


simulator.bindHandler("/children/invoke-function", function (message, session) {
    _logger.info("调用子设备功能:[{}]", message);
    session.sendMessage("/children/invoke-function", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        output: "ok", //返回结果
        success: true
    }));
});


simulator.bindHandler("/invoke-function", function (message, session) {
    _logger.info("调用设备功能:[{}]", message);
    session.sendMessage("/invoke-function", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        output: "ok", //返回结果
        success: true
    }));
});

simulator.bindHandler("/write-property", function (message, session) {
    var reply = com.alibaba.fastjson.JSON.toJSONString({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        headers:{"partialProperties":true},
        properties: new java.util.HashMap(message.properties),
        success: true
    });
    _logger.info("修改属性:{}\n{}", message, reply);

    session.sendMessage("/write-property", reply);
});


//订阅固件更新
simulator.bindHandler("/firmware/push", function (message, session) {
    _logger.info("固件更新:{}", message.toString());
    if (message.success === false) {

        return;
    }
    var progress = 0;
    var version = message.version;

    doUpgrade();

    function doUpgrade() {
        simulator.runDelay(function () {
            progress += 10;
            session.sendMessage("/firmware/progress", JSON.stringify({
                deviceId: message.deviceId,
                complete: progress >= 100, //更新完成
                progress: progress, //更新进度
                timestamp: new Date().getTime(),
                version: version, //更新版本号
                success: true
            }));
            if (progress >= 100) {
                return;
            }
            //模拟进度
            doUpgrade();
        }, 1000)
    }
});


simulator.onConnect(function (session) {

    // simulator.runDelay(function () {
    //
    //     session.sendMessage("/firmware/report", JSON.stringify({
    //         deviceId: session.auth.clientId,
    //         timestamp: new Date().getTime(),
    //         version: "1.0",
    //         success: true
    //     }));
    //
    //     session.sendMessage("/firmware/pull", JSON.stringify({
    //         deviceId: session.auth.clientId,
    //         timestamp: new Date().getTime(),
    //         version: "1.0",
    //         success: true
    //     }));
    // }, 2000)
    //自动注册
    // session.sendMessage("/register", JSON.stringify({
    //     deviceId: "aaaaabb",
    //     timestamp: new Date().getTime(),
    //     success: true,
    //     headers:{
    //         "productId":"demo-device",
    //         "deviceName":"自动注册"
    //     }
    // }));
    // simulator.runDelay(function () {
    //     session.sendMessage("/children/unregister", JSON.stringify({
    //         deviceId: "test202278",
    //         timestamp: new Date().getTime(),
    //         success: true
    //     }));
    // },2000)
});

simulator.onAuth(function (index, auth) {

    auth.setClientId("test001");
    auth.setUsername("test");
    auth.setPassword("test");
});