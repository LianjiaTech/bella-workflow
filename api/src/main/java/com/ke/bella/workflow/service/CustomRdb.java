package com.ke.bella.workflow.service;

import static com.ke.bella.workflow.service.Configs.INTERRUPTED_INTERVAL_ROWS;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.RowCountQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import groovy.lang.Closure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomRdb implements AutoCloseable {


    private HikariDataSource datasource;

    private JooqGroovyDSL dsl;

    @Override
    public void close() throws Exception {
        if (datasource != null) {
            datasource.close();
        }
    }

    public JooqGroovyDSL conn() {
        return dsl;
    }

    public static String createJdbcUrl(String dbType, String host, int port, String databaseName, Map<String, String> params) {
        StringBuilder url = new StringBuilder();
        switch (dbType) {
        case "mysql":
            url = url.append(String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName));
            break;
        case "postgresql":
            url = url.append(String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName));
            break;
        default:
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        if (params != null && !params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                url.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        return url.toString();
    }

    public static SQLDialect getSQLDialect(String dbType) {
        switch (dbType) {
        case "mysql":
            return SQLDialect.MYSQL;
        case "postgresql":
            return SQLDialect.POSTGRES;
        default:
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    public static HikariDataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMinimumIdle(0);

        return new HikariDataSource(config);
    }

    public static CustomRdb using(String dbType, String host, int port, String db, String user, String password, Map<String, String> params) {
        String jdbc = createJdbcUrl(dbType, host, port, db, params);
        HikariDataSource dss = createDataSource(jdbc, user, password);
        DSLContext dsl = DSL.using(dss, getSQLDialect(dbType));

        return CustomRdb.builder().dsl(new JooqGroovyDSL(dsl)).datasource(dss).build();
    }

    public static class JooqGroovyDSL {
        private final DSLContext dsl;

        public JooqGroovyDSL(DSLContext dsl) {
            this.dsl = dsl;
        }

        public void transaction(Closure<?> closure) {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                JooqGroovyDSL dslWrapper = new JooqGroovyDSL(ctx);
                closure.setDelegate(dslWrapper);
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                closure.call();
            });
        }

        public Iterator<Record> query(String sql, Object... bindings) {
            try (ResultQuery<Record> records = dsl.resultQuery(sql, bindings)) {
                records.fetchSize(Integer.MIN_VALUE);
                org.jooq.Cursor<Record> cursor = records.fetchLazy();

                return new CloseableIterator<Record>() {
                    @Override
                    public void close() throws Exception {
                        if(!cursor.isClosed()) {
                            cursor.close();
                        }
                    }

                    private int count = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor.hasNext();
                    }

                    @Override
                    public Record next() {
                        if(!hasNext()) {
                            try {
                                close();
                            } catch (Exception e) {
                                // ignore
                            }
                            throw new NoSuchElementException();
                        }
                        Record currentRecord = cursor.fetchNext();
                        count++;
                        if(count % INTERRUPTED_INTERVAL_ROWS == 0 && Thread.currentThread().isInterrupted()) {
                            try {
                                close();
                            } catch (Exception e) {
                                // ignore
                            }
                            throw new IllegalStateException();
                        }
                        return currentRecord;
                    }
                };
            }
        }

        private interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {
        }

        public int run(String sql, Object... bindings) {
            try (RowCountQuery query = dsl.query(sql, bindings)) {
                return query.execute();
            }
        }
    }
}
