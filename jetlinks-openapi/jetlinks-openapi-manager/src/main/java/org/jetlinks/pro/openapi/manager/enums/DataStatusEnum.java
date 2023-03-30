package org.jetlinks.pro.openapi.manager.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

/**
 * @author: gaoyf
 * @since: 1.0
 * @Date: 19-11-20 上午10:46
 */
@AllArgsConstructor
@Getter
@Dict("data-status-enum")
@JsonDeserialize(contentUsing = EnumDict.EnumDictJSONDeserializer.class)
public enum DataStatusEnum implements EnumDict<Byte> {
    ENABLED((byte) 1, "正常"),
    DISABLED((byte) 0, "禁用"),
    LOCK((byte) -1, "锁定"),
    DELETED((byte) -10, "删除");

    private Byte value;

    private String text;

}