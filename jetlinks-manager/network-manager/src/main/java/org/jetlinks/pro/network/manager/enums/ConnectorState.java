package org.jetlinks.pro.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
@Dict
public enum ConnectorState implements EnumDict<String> {
    running("运行中", "running"),
    paused("已暂停", "paused"),
    stopped("已停止", "stopped");

    private String text;

    private String value;

}