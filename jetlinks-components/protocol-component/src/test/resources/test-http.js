codec.decoder(function (context) {

    var message = context.getMessage();

    if (message.getUrl() === '/publish/device/message') {

        return message.ok( //响应http请求
            JSON.stringify({'success': true})
        )
            .thenReturn( //返回解析后的消息
                {
                    "messageType": "REPORT_PROPERTY",
                    "properties": {"property": "value"}
                }
            );
    }


    return null;

});

//编码读取属性
codec.encoder("READ_PROPERTY",function(context){

    var message = context.getMessage();

    return {
        "topic":"/read_property",
        "payload":JSON.stringify({"properties":[]})
    }

});