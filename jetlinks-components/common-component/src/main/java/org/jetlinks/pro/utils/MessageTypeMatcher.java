package org.jetlinks.pro.utils;

import org.jetlinks.core.message.MessageType;


public class MessageTypeMatcher extends EnumMatcher<MessageType> {

    public MessageTypeMatcher() {
        super(MessageType.values(), str -> MessageType.of(str.toUpperCase()));
    }
}