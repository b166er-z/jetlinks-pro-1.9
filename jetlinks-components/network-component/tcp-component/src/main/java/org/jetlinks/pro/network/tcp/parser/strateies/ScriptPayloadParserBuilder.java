package org.jetlinks.pro.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.network.tcp.parser.PayloadParser;
import org.jetlinks.pro.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.pro.network.tcp.parser.PayloadParserType;

import java.util.HashMap;
import java.util.Map;

/**
 * 利用jsr223脚本引擎和{@link PipePayloadParser}来描述解析器
 * <p>
 * 在脚本中可以使用内置变量: parser,对应类型为:{@link PipePayloadParser},如:
 *
 * <pre>
 * var BytesUtils = org.jetlinks.core.utils.BytesUtils;
 * parser.fixed(4)
 *        .handler(function(buffer){
 *             var len = BytesUtils.beToInt(buffer.getBytes());
 *             parser.fixed(len).result(buffer);
 *         })
 *        .handler(function(buffer){
 *             parser.result(buffer)
 *                    .complete();
 *         });
 * </pre>
 *
 * @author zhouhao
 * @since 1.0
 */
public class ScriptPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.SCRIPT;
    }

    @Override
    @SneakyThrows
    public PayloadParser build(ValueObject config) {
        String script = config.getString("script")
                              .orElseThrow(() -> new IllegalArgumentException("script不能为空"));
        String lang = config.getString("lang")
                            .orElseThrow(() -> new IllegalArgumentException("lang不能为空"));

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(lang);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的脚本:" + lang);
        }
        PipePayloadParser parser = new PipePayloadParser();
        String id = DigestUtils.md5Hex(script);
        if (!engine.compiled(id)) {
            engine.compile(id, script);
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("parser", parser);
        engine.execute(id, ctx).getIfSuccess();
        return parser;
    }
}
