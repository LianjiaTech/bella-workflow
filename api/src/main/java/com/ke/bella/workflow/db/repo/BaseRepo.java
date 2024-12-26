package com.ke.bella.workflow.db.repo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SelectLimitStep;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ke.bella.openapi.BellaContext;

public interface BaseRepo {
    default void fillCreatorInfo(Operator db) {
        com.ke.bella.openapi.Operator oper = BellaContext.getOperatorIgnoreNull();
        if(oper != null) {
            if(oper.getUserId() != null) {
                db.setCuid(oper.getUserId());
            }

            if(!StringUtils.isEmpty(oper.getUserName())) {
                db.setCuName(oper.getUserName());
            }
        }
        db.setCtime(LocalDateTime.now());
        fillUpdatorInfo(db);
    }

    default void fillUpdatorInfo(Operator db) {
        com.ke.bella.openapi.Operator oper = BellaContext.getOperatorIgnoreNull();
        if(oper != null) {
            if(oper.getUserId() != null) {
                db.setMuid(oper.getUserId());
            }

            if(!StringUtils.isEmpty(oper.getUserName())) {
                db.setMuName(oper.getUserName());
            }
        }
        db.setMtime(LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    default void batchExecuteQuery(DSLContext db, Collection<Query> queries) {
        int[] rows = db.batch(queries).execute();

        int sum = Arrays.stream(rows).sum();
        if(sum < queries.size()) {
            throw new IllegalStateException("批处理失败");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default <T> Page<T> queryPage(DSLContext db, SelectLimitStep scs, int page, int pageSize, Class<T> clazz) {
        if(scs == null) {
            return Page.from(page, pageSize);
        }
        return Page.from(page, pageSize)
                .total(db.fetchCount(scs))
                .list(scs.limit((page - 1) * pageSize, pageSize)
                        .fetch()
                        .into(clazz));
    }
}
