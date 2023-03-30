package org.jetlinks.pro.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.jetlinks.core.metadata.unit.ValueUnit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 自定义单位
 *
 * @author zhouhao
 * @since 1.2
 */
@AllArgsConstructor
@Getter
public enum CustomUnit implements ValueUnit, EnumDict<String> {

    kgce("Kg标准煤","kgce","能源单位","能源单位/kgce")
    ;

    private final String name;

    private final String symbol;

    private final String type;

    private final String description;

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String format(Object value) {
        return String.format("%s%s", value, getSymbol());
    }

    static Function<Object, String> template(String strTemplate) {
        return o -> String.format(strTemplate, o);
    }

    public static CustomUnit of(String value) {
        return Stream.of(CustomUnit.values())
            .filter(unifyUnit -> unifyUnit.getId().equals(value) || unifyUnit.getSymbol().equals(value))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getText() {
        return getName().concat("(").concat(getSymbol()+")");
    }

    @Override
    public Object getWriteJSONObject() {
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("id", this.getValue());
        jsonObject.put("value", this.getValue());
        jsonObject.put("text", this.getText());
        jsonObject.put("symbol", this.getSymbol());
        jsonObject.put("name", this.getName());
        jsonObject.put("type", this.getType());
        jsonObject.put("description", this.getDescription());
        return jsonObject;
    }
}
