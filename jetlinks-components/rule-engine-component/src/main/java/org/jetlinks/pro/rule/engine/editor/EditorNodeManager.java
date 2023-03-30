package org.jetlinks.pro.rule.engine.editor;

import reactor.core.publisher.Flux;

/**
 * 规则引擎编辑器节点管理器
 *
 * @author zhouhao
 * @since 1.9.0
 * @see EditorNodeProvider
 */
public interface EditorNodeManager {

    /**
     * 获取全部的编辑器节点
     *
     * @return 节点
     */
    Flux<EditorNode> getNodes();

}
