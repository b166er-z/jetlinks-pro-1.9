import org.jetlinks.pro.rule.engine.executor.DelayTaskExecutorProvider;
import org.jetlinks.rule.engine.api.RuleData;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

//package org.jetlinks.pro.standalone.configuration.fst;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
//import org.junit.jupiter.api.Test;
//import org.nustaq.serialization.FSTConfiguration;
//
//import static org.junit.jupiter.api.Assertions.*;
//
class FstSerializationRedisSerializerTest {
//
//
//    @Test
//    void test() {
//        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();
//        reply.setOutput(JSON.parseObject("{\"collectTm\":\"2020-11-17 01:10:09\",\"1\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"1\",\"value\":\"0\"},\"2\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"2\",\"value\":\"0\"},\"3\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"3\",\"value\":\"0\"},\"4\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"4\",\"value\":\"0\"},\"5\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"5\",\"value\":\"0\"},\"6\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"6\",\"value\":\"0\"},\"7\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"7\",\"value\":\"0\"},\"8\":{\"tm\":\"2020-11-17 01:10:09\",\"id\":\"8\",\"value\":\"0\"},\"gate_error\":\"error\"}"));
//
//        FstSerializationRedisSerializer serializer = new FstSerializationRedisSerializer(() -> {
//            FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration()
//                                                             .setForceSerializable(true);
//            configuration.setClassLoader(FstSerializationRedisSerializerTest.class.getClassLoader());
//            return configuration;
//        });
//
//        byte[] bytes = serializer.serialize(reply);
//
//        Object data = serializer.deserialize(bytes);
//
//        assertEquals(data.toString(),data.toString());
//    }

    @Test
    void test1(){

    }
}