package org.jetlinks.pro.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.tcp.parser.DirectRecordParser;
import org.jetlinks.pro.network.tcp.parser.PayloadParser;
import org.jetlinks.pro.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.pro.network.tcp.parser.PayloadParserType;

public class DirectPayloadParserBuilder implements PayloadParserBuilderStrategy {

    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DIRECT;
    }

    @Override
    @SneakyThrows
    public PayloadParser build(ValueObject config) {
        return new DirectRecordParser();
    }
}
