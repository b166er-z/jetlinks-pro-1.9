package org.jetlinks.pro.device.events.handler;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class ValueTypeTranslator {

    public static Object translator(Object value, DataType dataType) {
        try {
           if (dataType instanceof Converter<?>) {
                return ((Converter<?>) dataType).convert(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            log.error("设备上报值与物模型不匹配.value:{},type:{}", value, JSON.toJSONString(dataType), e);
            return value;
        }
    }

}
