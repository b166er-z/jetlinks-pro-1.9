package org.jetlinks.pro.notify.manager.subscriber;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor(staticName = "of")
@Getter
@Setter
@NoArgsConstructor
public class Notify {
    private String message;

    private String dataId;

    private long notifyTime;
}
