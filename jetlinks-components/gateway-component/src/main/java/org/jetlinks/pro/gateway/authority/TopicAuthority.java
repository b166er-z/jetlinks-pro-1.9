package org.jetlinks.pro.gateway.authority;

public enum TopicAuthority {
    NONE,
    PUB,
    SUB,
    ALL;

    public boolean has(TopicAuthority topicAuthority) {

        return this == ALL || this == topicAuthority;
    }
}
