package com.ke.bella.workflow.db.repo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SelectLimitStep;
import org.springframework.transaction.annotation.Transactional;

import com.ke.bella.workflow.db.BellaContext;

public interface BaseRepo {
    default void fillCreatorInfo(Operator db) {
        com.ke.bella.workflow.api.Operator oper = BellaContext.getOperator();
        if(oper != null) {
            db.setCuid(oper.getUserId());
            db.setCuName(oper.getUserName());
        }
        db.setCtime(LocalDateTime.now());
        fillUpdatorInfo(db);
    }

    default void fillUpdatorInfo(Operator db) {
        com.ke.bella.workflow.api.Operator oper = BellaContext.getOperator();
        if(oper != null) {
            db.setMuid(oper.getUserId());
            db.setMuName(oper.getUserName());
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
    public default <T> Page<T> queryPage(DSLContext db, SelectLimitStep scs, int page, int pageSize, Class<T> clazz) {
        if(scs == null) {
            return (Page<T>) Page.from(page, pageSize);
        }
        return Page.from(page, pageSize)
                .total(db.fetchCount(scs))
                .list(scs.limit((page - 1) * pageSize, pageSize)
                        .fetch()
                        .into(clazz));
    }
}
