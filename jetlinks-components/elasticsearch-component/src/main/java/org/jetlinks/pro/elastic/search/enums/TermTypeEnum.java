package org.jetlinks.pro.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.GeoShape;
import org.jetlinks.pro.elastic.search.geo.GeoIndexedShapeSearch;
import org.jetlinks.pro.elastic.search.utils.TermCommonUtils;
import org.jetlinks.pro.geo.*;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Jia_RG
 * @author bestfeng
 */
@Getter
@AllArgsConstructor
public enum TermTypeEnum {
    eq("eq") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.termQuery(term.getColumn().trim(), term.getValue());
        }
    },
    not("not") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(term.getColumn().trim(), term.getValue()));
        }
    },
    btw("btw") {
        @Override
        public QueryBuilder process(Term term) {
            Object between = null;
            Object and = null;
            List<?> values = TermCommonUtils.convertToList(term.getValue());
            if (values.size() > 0) {
                between = CastUtils.castNumber(values.get(0));
            }
            if (values.size() > 1) {
                and = CastUtils.castNumber(values.get(1));
            }
            return QueryBuilders.rangeQuery(term.getColumn().trim()).gte(between).lte(and);
        }
    },
    gt("gt") {
        @Override
        public QueryBuilder process(Term term) {
            Object value = CastUtils.castNumber(term.getValue());
            return QueryBuilders.rangeQuery(term.getColumn().trim()).gt(value);
        }
    },
    gte("gte") {
        @Override
        public QueryBuilder process(Term term) {
            Object value = CastUtils.castNumber(term.getValue());
            return QueryBuilders.rangeQuery(term.getColumn().trim()).gte(value);
        }
    },
    lt("lt") {
        @Override
        public QueryBuilder process(Term term) {
            Object value = CastUtils.castNumber(term.getValue());
            return QueryBuilders.rangeQuery(term.getColumn().trim()).lt(value);
        }
    },
    lte("lte") {
        @Override
        public QueryBuilder process(Term term) {
            Object value = CastUtils.castNumber(term.getValue());
            return QueryBuilders.rangeQuery(term.getColumn().trim()).lte(value);
        }
    },
    in("in") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.termsQuery(term.getColumn().trim(), TermCommonUtils.convertToList(term.getValue()));
        }
    },
    nin(TermType.nin) {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.boolQuery().mustNot(in.process(term));
        }
    },
    like("like") {
        @Override
        public QueryBuilder process(Term term) {
            //return QueryBuilders.matchPhraseQuery(term.getColumn().trim(), term.getValue());
            return QueryBuilders.wildcardQuery(term.getColumn().trim(), likeQueryTermValueHandler(term.getValue()));
        }
    },
    nlike("nlike") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders
                .boolQuery()
                .mustNot(QueryBuilders.wildcardQuery(term
                                                         .getColumn()
                                                         .trim(), likeQueryTermValueHandler(term.getValue())));
        }
    },
    geo("geo") {
        @Override
        @SneakyThrows
        public QueryBuilder process(Term term) {
            GeoSearch value = GeoSearch.of(term.getValue());
            if (value instanceof BoxSearch) {
                BoxSearch boxSearch = ((BoxSearch) value);
                return QueryBuilders.geoBoundingBoxQuery(term.getColumn().trim())
                                    .setCorners(
                                        new GeoPoint(boxSearch.getLeft().getLat(), boxSearch.getLeft().getLon()),
                                        new GeoPoint(boxSearch.getRight().getLat(), boxSearch.getRight().getLon())
                                    );
            }
            if (value instanceof CircleSearch) {
                CircleSearch circleSearch = ((CircleSearch) value);
                return QueryBuilders.geoDistanceQuery(term.getColumn().trim())
                                    .distance(circleSearch.getRadius().toString())
                                    .point(circleSearch.getCenter().getLat(), circleSearch.getCenter().getLon());
            }
            if (value instanceof PolygonSearch) {
                PolygonSearch polygonSearch = ((PolygonSearch) value);
                return QueryBuilders.geoPolygonQuery(
                    term.getColumn().trim()
                    , polygonSearch.getPoints()
                                   .stream()
                                   .map(point -> new GeoPoint(point.getLat(), point.getLat()))
                                   .collect(Collectors.toList())
                );
            }
            throw new IllegalArgumentException("不支持的geo参数");
        }
    },
    geoShape("geo-shape") {
        @Override
        @SneakyThrows
        public QueryBuilder process(Term term) {
            GeoSearch value = GeoSearch.of(term.getValue());
            ShapeRelation relation = ShapeRelation.valueOf(value.getRelation(GeoRelation.WITHIN).name());
            if (value instanceof BoxSearch) {
                BoxSearch boxSearch = ((BoxSearch) value);
                return QueryBuilders.geoShapeQuery(term.getColumn().trim(),
                                                   new EnvelopeBuilder(
                                                       new Coordinate(boxSearch.getLeft().getLon(), boxSearch
                                                           .getLeft()
                                                           .getLat()),
                                                       new Coordinate(boxSearch.getRight().getLon(), boxSearch
                                                           .getRight()
                                                           .getLat())
                                                   ))
                                    .relation(relation);
            } else if (value instanceof CircleSearch) {
                CircleSearch circleSearch = ((CircleSearch) value);
                return QueryBuilders.geoShapeQuery(term.getColumn().trim()
                    , new CircleBuilder().
                                             radius(circleSearch.getRadius().toString())
                                         .center(circleSearch.getCenter().getLon(), circleSearch.getCenter().getLat()))
                                    .relation(relation);
            } else if (value instanceof PolygonSearch) {
                PolygonSearch polygonSearch = ((PolygonSearch) value);
                return QueryBuilders.geoShapeQuery(
                    term.getColumn().trim()
                    , new PolygonBuilder(new CoordinatesBuilder()
                                             .coordinates(polygonSearch.getPoints()
                                                                       .stream()
                                                                       .map(point -> new Coordinate(point.getLon(), point
                                                                           .getLat()))
                                                                       .collect(Collectors.toList()))
                    )).relation(relation);
            } else if (value instanceof ShapeSearch) {
                ShapeSearch shapeSearch = ((ShapeSearch) value);
                GeoShape shape = shapeSearch.getShape();
                ShapeBuilder builder = null;

                Function<List<Object>, List<Coordinate>> geoPointConverter = list -> list
                    .stream()
                    .map(org.jetlinks.core.metadata.types.GeoPoint::of)
                    .map(point -> new Coordinate(point.getLon(), point.getLat()))
                    .collect(Collectors.toList());

                Function<List<Object>, PolygonBuilder> polygonBuilderFunction = list -> {
                    CoordinatesBuilder coordinatesBuilder = new CoordinatesBuilder();
                    for (Object coordinate : list) {
                        coordinatesBuilder.coordinates(geoPointConverter.apply(((List<Object>) coordinate)));
                    }
                    return new PolygonBuilder(coordinatesBuilder);
                };
                switch (shape.getType()) {
                    case Polygon:
                        builder = polygonBuilderFunction.apply(shape.getCoordinates());
                        break;
                    case MultiPolygon:
                        MultiPolygonBuilder multiPolygonBuilder = new MultiPolygonBuilder();
                        builder = multiPolygonBuilder;
                        for (Object coordinate : shape.getCoordinates()) {
                            multiPolygonBuilder.polygon(polygonBuilderFunction.apply(((List<Object>) coordinate)));
                        }
                        break;
                    case Point:
                        org.jetlinks.core.metadata.types.GeoPoint point = org.jetlinks.core.metadata.types.GeoPoint.of(shape
                                                                                                                           .getCoordinates());
                        builder = new PointBuilder().coordinate(new Coordinate(point.getLon(), point.getLat()));
                        break;
                    case MultiPoint:
                        builder = new MultiPointBuilder(geoPointConverter.apply(shape.getCoordinates()));
                        break;
                    case LineString:
                        relation = ShapeRelation.valueOf(value.getRelation(GeoRelation.INTERSECTS).name());
                        builder = new LineStringBuilder(geoPointConverter.apply(shape.getCoordinates()));
                        break;
                    case MultiLineString:
                        relation = ShapeRelation.valueOf(value.getRelation(GeoRelation.INTERSECTS).name());
                        MultiLineStringBuilder lineStringBuilder = new MultiLineStringBuilder();
                        builder = lineStringBuilder;
                        for (Object coordinate : shape.getCoordinates()) {
                            lineStringBuilder.coordinates(geoPointConverter.apply(((List<Object>) coordinate)));
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("不支持的类型:" + shape.getType());
                }

                return QueryBuilders.geoShapeQuery(term.getColumn().trim(), builder)
                                    .relation(relation);

            } else if (value instanceof GeoIndexedShapeSearch) {
                GeoIndexedShapeSearch objectSearch = ((GeoIndexedShapeSearch) value);

                return QueryBuilders.geoShapeQuery(term.getColumn().trim(), objectSearch.getId(), "_doc")
                                    .indexedShapePath(objectSearch.getPath())
                                    .relation(relation)
                                    .indexedShapeIndex(objectSearch.getIndex());
            }
            throw new IllegalArgumentException("不支持的geo参数");
        }
    };

    private final String type;

    public abstract QueryBuilder process(Term term);

    public static String likeQueryTermValueHandler(Object value) {
        if (!StringUtils.isEmpty(value)) {
            return value.toString().replace("%", "*");
        }
        return "**";
    }

    public static Optional<TermTypeEnum> of(String type) {
        return Arrays.stream(values())
                     .filter(e -> e.getType().equalsIgnoreCase(type))
                     .findAny();
    }
}
