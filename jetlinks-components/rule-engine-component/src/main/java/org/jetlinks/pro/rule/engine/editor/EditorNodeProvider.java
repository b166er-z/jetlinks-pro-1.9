package org.jetlinks.pro.rule.engine.editor;

import reactor.core.publisher.Flux;

/**
 * 规则引擎编辑器节点提供商,用于提供自定义编辑器节点信息
 *
 * @author zhouhao
 * @since 1.9
 */
public interface EditorNodeProvider {

    Flux<EditorNode> getNodes();

}
