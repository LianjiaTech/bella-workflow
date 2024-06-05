package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.WorkflowRun.*;
import static com.ke.bella.workflow.db.tables.WorkflowNodeRun.*;

import java.util.regex.Pattern;

import org.jooq.DSLContext;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.tools.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class DSLContextHolder {
    private static final Cache<String, DSLContext> configurations = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    public static synchronized DSLContext get(String key, final DSLContext db) {
        if(StringUtils.isEmpty(key)) {
            return db;
        }

        DSLContext ret = configurations.getIfPresent(key);
        if(ret == null) {
            ret = DSL.using(db.configuration().derive(newSettings(key)));
            configurations.put(key, ret);
        }

        return ret;
    }

    public static Settings newSettings(String key) {
        return new Settings().withRenderMapping(new RenderMapping()
                .withSchemata(
                        new MappedSchema()
                                .withInputExpression(Pattern.compile(".*"))
                                .withTables(new MappedTable()
                                        .withInput(WORKFLOW_RUN.getName())
                                        .withOutput(targetTableName(WORKFLOW_RUN.getName(), key)),
                                        new MappedTable()
                                                .withInput(WORKFLOW_NODE_RUN.getName())
                                                .withOutput(targetTableName(WORKFLOW_NODE_RUN.getName(), key)))));
    }

    public static String targetTableName(String orignalName, String key) {
        if(StringUtils.isEmpty(key)) {
            return orignalName;
        }
        return String.format("%s_%s", orignalName, key);
    }
}
