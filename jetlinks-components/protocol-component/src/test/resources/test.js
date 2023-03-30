codec.decoder(function (context) {

    var message = context.getMessage();

    if(message.getTopic()==='/test'){
        return {
            "messageType":"EVENT",
            "event":"test",
            "data":"test"
        }
    }


    return null;

});


codec.encoder("READ_PROPERTY",function(context){

});