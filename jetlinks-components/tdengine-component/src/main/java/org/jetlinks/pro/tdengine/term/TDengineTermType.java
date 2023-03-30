package org.jetlinks.pro.tdengine.term;

import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum TDengineTermType {
    is(true, "="),
    eq(true, "="),
    not(true, "!="),
    gt(true, ">"),
    gte(true, ">="),
    lt(true, "<"),
    lte(true, "<="),
    like(false, "like") {
        @Override
        protected void doBuild(String column, Object value, StringJoiner sql) {
            String val = String.valueOf(value);
            sql.add(escapeColumn(column))
                .add(" like ").add(val);
        }
    },
    btw(true, "btw") {
        @Override
        protected void doBuild(String column, Object value, StringJoiner sql) {
            List<Object> values = new ArrayList<>(convertList(value));
            if (values.isEmpty()) {
                return;
            }
            gte.build(column, values.get(0), sql);
            if (values.size() >= 2) {
                sql.add(" and ");
                lte.build(column, values.get(1), sql);
            }

        }
    },
    in(false, "in") {
        @Override
        protected void doBuild(String column, Object value, StringJoiner sql) {
            String colSql =  escapeColumn(column);

            sql.add(convertList(value)
                .stream()
                .map(v -> colSql + " = " + createValue(v))
                .collect(Collectors.joining(" or ", "(", ")")));
        }
    },
    nin(false, "nin") {
        @Override
        protected void doBuild(String column, Object value, StringJoiner sql) {
            String colSql =escapeColumn(column) ;
            sql.add(convertList(value)
                .stream()
                .map(v -> colSql + " != " + createValue(v))
                .collect(Collectors.joining(" and ", "(", ")")));
        }
    };


    boolean forNumber;
    String expr;

    public static Collection<Object> convertList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof String) {
            value = ((String) value).split("[,]");
        }

        if (value instanceof Object[]) {
            value = Arrays.asList(((Object[]) value));
        }

        if (value instanceof Collection) {
            return ((Collection<Object>) value);
        }

        return Collections.singletonList(value);
    }

    protected String escapeValue(String value) {
        return value.replace("'", "\\'");
    }

    protected String escapeColumn(String value) {
        return value;
    }

    protected String createValue(Object value) {
        String strVal = escapeValue(value.toString());
        if (value instanceof Number) {
            return value.toString();
        } else if (strVal.startsWith("'") && strVal.endsWith("'")) {
            return strVal;
        } else {
            return "'" + strVal + "'";
        }
    }

    protected void doBuild(String column, Object value, StringJoiner sql) {
        sql.add(escapeColumn(column))
            .add(" ")
            .add(expr)
            .add(" ").add(createValue(value));
    }

    public String build(String column, Object value) {
        StringJoiner joiner = new StringJoiner("");
        build(column, value, joiner);
        return joiner.toString();
    }

    public void build(String column, Object value, StringJoiner sql) {
        if (StringUtils.isEmpty(column) || value == null) {
            return;
        }

        doBuild(column, value, sql);
    }
}
