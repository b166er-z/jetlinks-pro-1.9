package org.jetlinks.pro.utils;

import org.jetlinks.core.message.MessageType;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MessageTypeMatcherTest {

    @Test
    void testDefault() {
        MessageTypeMatcher messageTypeMatcher = new MessageTypeMatcher();
        messageTypeMatcher.init();

        for (MessageType value : MessageType.values()) {
            assertTrue(messageTypeMatcher.match(value));
        }

    }

    @Test
    void testExclude() {
        MessageTypeMatcher messageTypeMatcher = new MessageTypeMatcher();
        messageTypeMatcher.setExcludes(Collections.singleton("EVENT"));
        messageTypeMatcher.setIncludes(Collections.singleton("*"));
        messageTypeMatcher.setExcludeFirst(true);

        assertFalse(messageTypeMatcher.match(MessageType.EVENT));
        assertTrue(messageTypeMatcher.match(MessageType.READ_PROPERTY));

    }

    @Test
    void testInclude() {
        MessageTypeMatcher messageTypeMatcher = new MessageTypeMatcher();
        messageTypeMatcher.setExcludes(Collections.singleton("*"));
        messageTypeMatcher.setIncludes(Collections.singleton("EVENT"));
        messageTypeMatcher.setExcludeFirst(false);

        assertTrue(messageTypeMatcher.match(MessageType.EVENT));
        assertFalse(messageTypeMatcher.match(MessageType.READ_PROPERTY));

    }
}