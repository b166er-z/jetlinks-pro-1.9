package org.jetlinks.pro.network.tcp.parser.strateies;

import io.vertx.core.parsetools.RecordParser;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.tcp.parser.PayloadParserType;

/**
 * 固定长度解析器构造器,每次读取固定长度的数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class FixLengthPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.FIXED_LENGTH;
    }

    @Override
    protected RecordParser createParser(ValueObject config) {
        return RecordParser.newFixed(config.getInt("size")
                                           .orElseThrow(() -> new IllegalArgumentException("size can not be null")));
    }


}
