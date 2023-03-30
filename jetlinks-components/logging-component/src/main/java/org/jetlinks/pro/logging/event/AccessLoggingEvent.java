package org.jetlinks.pro.logging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.logging.access.SerializableAccessLog;

@Getter
@Setter
@AllArgsConstructor
public class AccessLoggingEvent {
    SerializableAccessLog log;
}
