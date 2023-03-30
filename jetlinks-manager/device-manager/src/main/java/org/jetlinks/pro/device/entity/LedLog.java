package org.jetlinks.pro.device.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LedLog implements Serializable {
    private String ledId;
    private String deviceId;
    private String name;
    private String value;
    private String writeCmd;
    private String result;
    private long time;


}
