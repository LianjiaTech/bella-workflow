package com.ke.bella.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.api.DataSourceOps.DataSourceOp;
import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.RdbDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.RedisDataSourceAdd;
import com.ke.bella.workflow.api.WorkflowOps.DomainAdd;
import com.ke.bella.workflow.db.repo.DataSourceRepo;
import com.ke.bella.workflow.db.tables.pojos.DomainDB;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.RdbDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.RedisDatasourceDB;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.Utils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@DependsOn("configs")
public class DataSourceService implements ApplicationContextAware {

    @Resource
    DataSourceRepo repo;

    AtomicReference<Set<String>> customDomains = new AtomicReference<>(new HashSet<>());

    RemovalListener<String, AutoCloseable> removalListener = notification -> {
        AutoCloseable value = notification.getValue();
        try {
            value.close();
        } catch (Exception e) {
            // ignore
        }
    };

    LoadingCache<String, CustomKafkaProducer> kafkaCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1024)
            .removalListener(removalListener)
            .build(CacheLoader.from(k -> {
                KafkaDatasourceDB e = getKafkaDatasource(k, "producer");
                Assert.notNull(e, "找不到对应的数据源: " + k);
                return CustomKafkaProducer.using(e.getServer());
            }));

    LoadingCache<String, CustomRdb> rdbCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1024)
            .removalListener(removalListener)
            .build(CacheLoader.from(k -> {
                RdbDatasourceDB e = getRdbDatasource(k);
                Assert.notNull(e, "找不到对应的数据源: " + k);
                return CustomRdb.using(e.getDbType(), e.getHost(), e.getPort(), e.getDb(), e.getUser(), e.getPassword(),
                        JsonUtils.fromJson(e.getParams(), new TypeReference<Map<String, String>>(){ }));
            }));

    LoadingCache<String, CustomRedis> redisCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1024)
            .removalListener(removalListener)
            .build(CacheLoader.from(k -> {
                RedisDatasourceDB e = getRedisDatasource(k);
                Assert.notNull(e, "找不到对应的数据源: " + k);
                return CustomRedis.using(e.getHost(), e.getPort(), e.getUser(), e.getPassword(), e.getDb());
            }));

    private static ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        TaskExecutor.schedule(this::refreshCustomDomains, 1000);

        // try refresh domains every 60s.
        TaskExecutor.scheduleAtFixedRate(this::refreshCustomDomains, 120);
    }

    public KafkaDatasourceDB createKafkaDs(KafkaDataSourceAdd op) {
        return repo.addKafkaDs(op);
    }

    public void removeKafkaDs(DataSourceOp op) {
        repo.removeKafkaDs(op);
    }

    public Object listDataSources(String type) {
        if("kafka".equals(type)) {
            return repo.listTenantAllActiveKafkaDs();
        }
        return null;
    }

    public List<DomainDB> listDomains(String prefix) {
        return repo.listDomains(prefix);
    }

    public DomainDB addDomain(DomainAdd domainOp) {
        return repo.addDomain(domainOp);
    }

    public boolean isCustomDomain(String host) {
        return customDomains.get().contains(host);
    }

    private void refreshCustomDomains() {
        List<String> domains = repo.listAllCustomDomains();
        customDomains.set(new HashSet<>(domains));
    }

    public KafkaDatasourceDB getKafkaDatasource(String datasourceId, String type) {
        return repo.queryKafkaDs(datasourceId, type);
    }

    public CustomKafkaProducer acquireCustomKafkaProducer(String datasourceId) {
        try {
            return kafkaCache.get(datasourceId);
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(Utils.getRootCause(e));
        }
    }

    public RdbDatasourceDB getRdbDatasource(String datasourceId) {
        return repo.queryRdbDataSource(datasourceId);
    }

    public RdbDatasourceDB createRdbDatasource(RdbDataSourceAdd op) {
        checkRdbDatasource(op);
        return repo.addRdbDataSource(op);
    }

    public void removeRdbDatasource(DataSourceOp op) {
        repo.removeRdbDatasource(op);
    }

    public void checkRdbDatasource(RdbDataSourceAdd op) {
        try (CustomRdb rdb = CustomRdb.using(op.getDbType(),
                op.getHost(), op.getPort(), op.getDb(), op.getUser(), op.getPassword(), op.getParams())) {
            rdb.conn().execute("select 1;");
        } catch (Throwable e) {
            e = Utils.getRootCause(e);
            throw new IllegalArgumentException("校验不通过:" + e.getMessage(), e);
        }
    }

    public CustomRdb acquireCustomRdb(String datasourceId) {
        try {
            return rdbCache.get(datasourceId);
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(Utils.getRootCause(e));
        }
    }

    public RedisDatasourceDB getRedisDatasource(String datasourceId) {
        return repo.queryRedisDataSource(datasourceId);
    }

    public RedisDatasourceDB createRedisDatasource(RedisDataSourceAdd op) {
        checkRedisDatasource(op);
        return repo.addRedisDataSource(op);
    }

    public void removeRedisDatasource(DataSourceOp op) {
        repo.removeRedisDatasource(op);
    }

    public void checkRedisDatasource(RedisDataSourceAdd op) {
        try (CustomRedis rdb = CustomRedis.using(op.getHost(), op.getPort(), op.getUser(), op.getPassword(), op.getDb())) {
            rdb.conn().get("mykey");
        } catch (Throwable e) {
            e = Utils.getRootCause(e);
            throw new IllegalArgumentException("校验不通过:" + e.getMessage(), e);
        }
    }

    public CustomRedis acquireCustomRedis(String datasourceId) {
        try {
            return redisCache.get(datasourceId);
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(Utils.getRootCause(e));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static DataSourceService ds() {
        return applicationContext.getBean(DataSourceService.class);
    }
}
