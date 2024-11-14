package com.ke.bella.workflow.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.api.DataSourceOps.DataSourceOp;
import com.ke.bella.workflow.api.DataSourceOps.KafkaDataSourceAdd;
import com.ke.bella.workflow.api.DataSourceOps.RdbDataSourceAdd;
import com.ke.bella.workflow.api.WorkflowOps.DomainAdd;
import com.ke.bella.workflow.db.repo.DataSourceRepo;
import com.ke.bella.workflow.db.tables.pojos.DomainDB;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.RdbDatasourceDB;
import com.ke.bella.workflow.utils.Utils;

@Component
@DependsOn("configs")
public class DataSourceService implements ApplicationContextAware {

    @Resource
    DataSourceRepo repo;

    AtomicReference<Set<String>> customDomains = new AtomicReference<>(new HashSet<>());

    RemovalListener<String, CustomRdb> removalListener = new RemovalListener<String, CustomRdb>() {
        @Override
        public void onRemoval(RemovalNotification<String, CustomRdb> notification) {
            CustomRdb value = notification.getValue();
            try {
                value.close();
            } catch (Exception e) {
                // ignore
            }
        }
    };

    LoadingCache<String, CustomRdb> rdbCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1024)
            .removalListener(removalListener)
            .build(CacheLoader.<String, CustomRdb>from(k -> {
                RdbDatasourceDB e = getRdbDatasource(k);
                Assert.notNull(e, "找不到对应的数据源: " + k);
                return CustomRdb.using(e.getDbType(), e.getHost(), e.getPort(), e.getDb(), e.getUser(), e.getPassword());
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

    public RdbDatasourceDB getRdbDatasource(String datasourceId) {
        return repo.queryRdbDataSource(datasourceId);
    }

    public RdbDatasourceDB createRdbDatasource(RdbDataSourceAdd op) {
        return repo.addRdbDataSource(op);
    }

    public void removeRdbDatasource(DataSourceOp op) {
        repo.removeRdbDatasource(op);
    }

    public void checkRdbDatasource(RdbDataSourceAdd op) {
        try (CustomRdb rdb = CustomRdb.using(op.getDbType(),
                op.getHost(), op.getPort(), op.getDb(), op.getUser(), op.getPassword())) {
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

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static DataSourceService ds() {
        return applicationContext.getBean(DataSourceService.class);
    }
}
