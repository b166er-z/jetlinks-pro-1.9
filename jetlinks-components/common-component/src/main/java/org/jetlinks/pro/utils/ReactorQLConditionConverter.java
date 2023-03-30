package org.jetlinks.pro.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FromFeature;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.supports.ExpressionVisitorAdapter;
import org.jetlinks.reactor.ql.utils.SqlUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Consumer3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReactorQLConditionConverter {

    private final Function<ConditionBuilder, Mono<Void>> builder;

    public Mono<QueryParamEntity> convertQueryParam(ReactorQLRecord record) {
        return convertQueryParam(record, new QueryParamEntity());
    }

    public Mono<QueryParamEntity> convertQueryParam(ReactorQLRecord record, QueryParamEntity param) {
        return Mono.defer(() -> {
            Query<?, QueryParamEntity> query = param.toQuery();

            ConditionBuilder conditionBuilder = ConditionBuilder.create(record, query);

            return this
                .builder.apply(conditionBuilder)
                .then(Mono.fromSupplier(query::getParam));
        });
    }

    public static ReactorQLConditionConverter create(Expression expression, ReactorQLMetadata metadata) {
        return new ReactorQLConditionConverter(createConditionBuilder(expression, metadata));
    }

    private static final Function<ConditionBuilder, Mono<Void>> emptyBuilder = builder -> Mono.empty();

    static class ConditionBuilder {
        ReactorQLRecord record;
        Consumer3<String, String, Object> condition;
        Runnable or;
        Runnable and;
        Supplier<NestConditional<?>> nestSupplier;

        boolean isOr;

        public void or() {
            isOr = true;
            or.run();
        }

        public void and() {
            isOr = false;
            and.run();
        }

        public static ConditionBuilder create(ReactorQLRecord record, Conditional<?> conditional) {
            ConditionBuilder builder = new ConditionBuilder();
            builder.record = record;
            builder.condition = conditional::accept;
            builder.and = conditional::and;
            builder.or = conditional::or;
            builder.nestSupplier = () -> builder.isOr ? conditional.orNest() : conditional.nest();
            return builder;
        }

        public ConditionBuilder nest() {
            ConditionBuilder builder = new ConditionBuilder();
            NestConditional<?> nest = nestSupplier.get();
            builder.record = record;
            builder.condition = nest::accept;
            builder.or = nest::or;
            builder.and = nest::and;
            builder.nestSupplier = () -> builder.isOr ? nest.orNest() : nest.nest();
            return builder;
        }
    }

    private static String getColumnByExpression(Expression expr) {
        if (expr instanceof Column) {
            return SqlUtils.getCleanStr(((Column) expr).getColumnName());
        } else {
            throw new IllegalArgumentException("不支持的列名表达式:" + expr);
        }
    }

    private static Function<ConditionBuilder, Mono<Void>> createConditionBuilder(Expression expression, ReactorQLMetadata metadata) {
        if (expression == null) {
            return emptyBuilder;
        }
        List<Function<ConditionBuilder, Publisher<?>>> mappers = new ArrayList<>();

        BiConsumer<BinaryExpression, String> accepter = (expr, termType) -> {
            Expression left = expr.getLeftExpression();
            Expression right = expr.getRightExpression();
            String column = getColumnByExpression(left);
            Function<ReactorQLRecord, ? extends Publisher<?>> value = ValueMapFeature.createMapperNow(right, metadata);

            mappers.add((builder) -> Mono
                .from(value.apply(builder.record))
                .doOnNext(val -> builder.condition.accept(column, termType, val)));
        };

        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(AndExpression expr) {
                Expression left = expr.getLeftExpression();
                Expression right = expr.getRightExpression();
                Function<ConditionBuilder, Mono<Void>> leftBuilder = createConditionBuilder(left, metadata);
                Function<ConditionBuilder, Mono<Void>> rightBuilder = createConditionBuilder(right, metadata);

                mappers.add(builder -> leftBuilder.apply(builder)
                    .then(Mono.fromRunnable(builder::and))
                    .then(rightBuilder.apply(builder)));

            }

            @Override
            public void visit(OrExpression expr) {
                Expression left = expr.getLeftExpression();
                Expression right = expr.getRightExpression();
                Function<ConditionBuilder, Mono<Void>> leftBuilder = createConditionBuilder(left, metadata);
                Function<ConditionBuilder, Mono<Void>> rightBuilder = createConditionBuilder(right, metadata);

                mappers.add(builder -> leftBuilder.apply(builder)
                    .then(Mono.fromRunnable(builder::or))
                    .then(rightBuilder.apply(builder)));

            }

            @Override
            public void visit(Parenthesis parenthesis) {
                Expression expr = parenthesis.getExpression();
                Function<ConditionBuilder, Mono<Void>> func = createConditionBuilder(expr, metadata);
                mappers.add(builder -> func.apply(builder.nest()));
            }

            @Override
            public void visit(EqualsTo expr) {
                accepter.accept(expr, TermType.eq);
            }

            @Override
            public void visit(NotEqualsTo expr) {
                accepter.accept(expr, TermType.not);
            }

            @Override
            public void visit(GreaterThan expr) {
                accepter.accept(expr, TermType.gt);
            }

            @Override
            public void visit(GreaterThanEquals expr) {
                accepter.accept(expr, TermType.gte);
            }

            @Override
            public void visit(MinorThan expr) {
                accepter.accept(expr, TermType.lt);
            }

            @Override
            public void visit(MinorThanEquals expr) {
                accepter.accept(expr, TermType.lte);
            }

            @Override
            public void visit(LikeExpression expr) {
                accepter.accept(expr, TermType.like);
            }

            @Override
            public void visit(InExpression expr) {
                Expression left = expr.getLeftExpression();
                String column = getColumnByExpression(left);
                ItemsList list = expr.getRightItemsList();
                AtomicReference<Function<ReactorQLRecord, ? extends Publisher<?>>> ref = new AtomicReference<>();
                list.accept(new ItemsListVisitor() {
                    @Override
                    public void visit(SubSelect subSelect) {

                        Function<ReactorQLContext, Flux<ReactorQLRecord>> mapper = FromFeature.createFromMapperByBody(subSelect.getSelectBody(), metadata);

                        ref.set((record) -> mapper.apply(record.getContext())
                            .map(ReactorQLRecord::asMap)
                            .filter(MapUtils::isNotEmpty)
                            .map(result -> result.values().iterator().next())
                            .collectList());

                    }

                    @Override
                    public void visit(ExpressionList expressionList) {
                        List<Function<ReactorQLRecord, ? extends Publisher<?>>> all =
                            expressionList.getExpressions()
                                .stream()
                                .map(expr -> ValueMapFeature.createMapperNow(expr, metadata))
                                .collect(Collectors.toList());

                        ref.set(record -> Flux.fromIterable(all)
                            .flatMap(mapper -> mapper.apply(record)));
                    }

                    @Override
                    public void visit(NamedExpressionList namedExpressionList) {

                    }

                    @Override
                    public void visit(MultiExpressionList multiExprList) {

                    }
                });
                if (ref.get() == null) {
                    throw new IllegalArgumentException("不支持到表达式:" + expr);
                }
                mappers.add(builder -> Flux
                    .from(ref.get().apply(builder.record))
                    .collectList()
                    .doOnNext(args -> builder.condition.accept(column, "in", args)));
            }

            @Override
            public void visit(Between expr) {
                Expression left = expr.getLeftExpression();
                Expression between = expr.getBetweenExpressionStart();
                Expression and = expr.getBetweenExpressionEnd();
                String column = getColumnByExpression(left);

                Function<ReactorQLRecord, ? extends Publisher<?>> betweenFunc = ValueMapFeature.createMapperNow(between, metadata);
                Function<ReactorQLRecord, ? extends Publisher<?>> andFunc = ValueMapFeature.createMapperNow(and, metadata);
                mappers.add(record -> Mono
                    .zip(
                        Mono.from(betweenFunc.apply(record.record)),
                        Mono.from(andFunc.apply(record.record))
                    )
                    .doOnNext(tp2 -> record.condition.accept(column, TermType.btw, Arrays.asList(tp2.getT1(), tp2.getT2()))));
            }

        });

        if (mappers.isEmpty()) {
            throw new IllegalArgumentException("不支持到表达式" + expression);
        }

        return builder -> Flux
            .fromIterable(mappers)
            .concatMap(mapper -> mapper.apply(builder))
            .then();

    }

}
