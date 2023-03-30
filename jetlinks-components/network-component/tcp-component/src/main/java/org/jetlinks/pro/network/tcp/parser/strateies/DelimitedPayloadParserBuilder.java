package org.jetlinks.pro.network.tcp.parser.strateies;

import io.vertx.core.parsetools.RecordParser;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.tcp.parser.PayloadParserType;

/**
 * 以分隔符读取数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class DelimitedPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DELIMITED;
    }

    @Override
    protected RecordParser createParser(ValueObject config) {

        return RecordParser
            .newDelimited(StringEscapeUtils
                              .unescapeJava(config
                                                .getString("delimited")
                                                .orElseThrow(() -> new IllegalArgumentException("delimited can not be null"))));
    }


}
