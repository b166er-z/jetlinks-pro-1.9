package org.jetlinks.pro.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.Term;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;

/**
 * @author Jia_RG
 */
@Getter
@AllArgsConstructor
public enum LinkTypeEnum {

    and("and") {
        @Override
        public void process(BoolQueryBuilder query, Term term) {
            if (term.getTerms().isEmpty()) {
                query.must(TermTypeEnum
                    .of(term.getTermType().trim())
                    .map(e -> createQueryBuilder(e,term))
                    .orElse(QueryBuilders.boolQuery()));
            } else {
                // 嵌套查询新建一个包起来
                BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
                LinkedList<Term> terms = ((LinkedList<Term>) term.getTerms());
                // 同一层级取最后一个的type
                LinkTypeEnum.of(getLast(terms).getType().name()).ifPresent(e -> terms.forEach(t -> e.process(nextQuery, t)));
                // 处理完后包括进去
                query.must(nextQuery);
            }
        }
    },
    or("or") {
        @Override
        public void process(BoolQueryBuilder query, Term term) {
            // 跟上面代码相似
            if (term.getTerms().isEmpty()) {
                query.should(TermTypeEnum.of(term.getTermType().trim()).map(e -> createQueryBuilder(e,term)).orElse(QueryBuilders.boolQuery()));
            } else {
                BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
                LinkedList<Term> terms = ((LinkedList<Term>) term.getTerms());
                LinkTypeEnum.of(getLast(terms).getType().name()).ifPresent(e -> terms.forEach(t -> e.process(nextQuery, t)));
                query.should(nextQuery);
            }
        }
    };

    private final String type;

    public abstract void process(BoolQueryBuilder query, Term term);

    public static QueryBuilder createQueryBuilder(TermTypeEnum linkTypeEnum, Term term) {
        if (term.getColumn().contains(".")) {
            return new NestedQueryBuilder(term.getColumn().split("[.]")[0],linkTypeEnum.process(term), ScoreMode.Max);
        }
        return linkTypeEnum.process(term);
    }

    public static Optional<LinkTypeEnum> of(String type) {
        return Arrays.stream(values())
            .filter(e -> e.getType().equalsIgnoreCase(type))
            .findAny();
    }

    private static Term getLast(LinkedList<Term> terms) {
        int index = terms.indexOf(terms.getLast());
        while (index >= 0) {
            if (terms.get(index).getTerms().isEmpty()) break;
            index--;
        }
        return terms.get(index);
    }
}
