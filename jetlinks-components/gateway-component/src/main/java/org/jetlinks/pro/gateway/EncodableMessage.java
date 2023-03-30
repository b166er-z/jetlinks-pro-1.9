package org.jetlinks.pro.gateway;

import org.jetlinks.core.message.codec.EncodedMessage;

public interface EncodableMessage extends EncodedMessage {

    Object getNativePayload();

    static EncodableMessage of(Object object) {
        return new JsonEncodedMessage(object);
    }
}
