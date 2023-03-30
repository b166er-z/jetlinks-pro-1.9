package org.jetlinks.pro.tdengine;

import io.vavr.CheckedConsumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hswebframework.ezorm.rdb.executor.jdbc.JdbcSqlExecutorHelper.getResultColumns;

@AllArgsConstructor
public class JdbcTDengineOperations implements TDengineOperations {

    private final DataSource dataSource;

    private final Scheduler scheduler;

    @SneakyThrows
    public void doWithConnection(CheckedConsumer<Connection> consumer) {

        try (Connection connection = dataSource.getConnection()) {
            consumer.accept(connection);
        }
    }

    @Override
    public Mono<Void> execute(String sql) {

        return Mono
            .<Void>fromRunnable(() -> doWithConnection(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }))
            .subscribeOn(scheduler);
    }

    @Override
    public Flux<Map<String, Object>> query(String sql) {

        return Flux
            .<Map<String, Object>>create(sink -> doWithConnection(connection -> {
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery(sql)) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        List<String> columns = getResultColumns(metaData);
                        while (resultSet.next() && !sink.isCancelled()) {
                            Map<String, Object> result = new LinkedHashMap<>();
                            //调用包装器,将查询结果包装为对象
                            for (String column : columns) {
                                Object value = resultSet.getObject(column);
                                result.put(column, value);
                            }
                            sink.next(result);
                        }
                        sink.complete();
                    }
                }

            }))
            .subscribeOn(scheduler);
    }
}
