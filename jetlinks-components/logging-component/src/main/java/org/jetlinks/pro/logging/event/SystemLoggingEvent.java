package org.jetlinks.pro.logging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.logging.system.SerializableSystemLog;

@Getter
@Setter
@AllArgsConstructor
public class SystemLoggingEvent {
    SerializableSystemLog log;
}
