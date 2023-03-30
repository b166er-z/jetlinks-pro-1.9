package org.jetlinks.pro.gateway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.rule.engine.executor.PayloadType;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JsonEncodedMessage implements EncodableMessage {

    private transient volatile ByteBuf payload;

    @Getter
    private final Object nativePayload;

    public JsonEncodedMessage(Object nativePayload) {
        Objects.requireNonNull(nativePayload);
        this.nativePayload = nativePayload;
    }

    @Override
    public JSONArray payloadAsJsonArray() {
        return (JSONArray) JSON.toJSON(nativePayload);
    }

    @Override
    public JSONObject payloadAsJson() {
        return (JSONObject) JSON.toJSON(nativePayload);
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        if (payload == null) {
            payload = PayloadType.JSON.write(nativePayload);
        }
        return payload;
    }


}
