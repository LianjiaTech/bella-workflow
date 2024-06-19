package com.ke.bella.workflow.db.repo;

import static com.ke.bella.workflow.db.tables.Instance.INSTANCE;

import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ke.bella.workflow.db.tables.records.InstanceRecord;

@Component
public class InstanceRepo {
    @Resource
    private DSLContext db;

    @Transactional
    public Long register(String ip, int port) {
        InstanceRecord rec = db.selectFrom(INSTANCE)
                .where(INSTANCE.IP.eq(ip).and(INSTANCE.PORT.eq(port))).fetchOne();
        if(rec == null) {
            rec = findIdle();
            rec.set(INSTANCE.IP, ip);
            rec.set(INSTANCE.PORT, port);
        }
        rec.setStatus(1);
        rec.set(INSTANCE.MTIME, LocalDateTime.now());
        rec.store();
        return rec.getId();
    }

    public void unregister(String ip, int port) {
        InstanceRecord rec = db.selectFrom(INSTANCE)
                .where(INSTANCE.IP.eq(ip).and(INSTANCE.PORT.eq(port))).fetchOne();
        if(rec != null) {
            rec.setStatus(0);
            rec.set(INSTANCE.MTIME, LocalDateTime.now());
            rec.store();
        }
    }

    private InstanceRecord findIdle() {
        InstanceRecord rec = db.selectFrom(INSTANCE)
                .where(INSTANCE.STATUS.eq(0)).limit(1).fetchAny();
        if(rec == null) {
            rec = INSTANCE.newRecord();
            rec.set(INSTANCE.CTIME, LocalDateTime.now());
            rec.attach(db.configuration());
        }
        return rec;
    }

    public void forUpdateInstance1() {
        db.select(INSTANCE.ID).from(INSTANCE).where(INSTANCE.ID.eq(1L)).forUpdate().fetch();
    }
}
