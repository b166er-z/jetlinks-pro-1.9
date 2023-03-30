package org.jetlinks.pro.influx.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.AbstractTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class InfluxDBQueryConditionBuilder extends AbstractTermsFragmentBuilder<Object> {


    public static String build(List<Term> terms) {

        if(CollectionUtils.isEmpty(terms)){
            return "";
        }
        SqlFragments fragments = new InfluxDBQueryConditionBuilder().createTermFragments(null, terms);

        if(fragments.isEmpty()){
            return "";
        }

        return fragments.toRequest().toString();

    }

    @Override
    protected SqlFragments createTermFragments(Object parameter, Term term) {
        String type = term.getTermType();
        InfluxDBTermType termType = InfluxDBTermType.valueOf(type.toLowerCase());

        return SqlFragments.single(termType.build(term.getColumn(), term.getValue()));
    }
}
