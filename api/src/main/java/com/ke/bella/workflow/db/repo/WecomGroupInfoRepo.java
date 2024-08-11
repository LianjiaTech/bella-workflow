package com.ke.bella.workflow.db.repo;

import java.util.Objects;
import org.jooq.impl.DSL;
import org.jooq.Condition;
import org.jooq.DSLContext;
import java.time.LocalDateTime;
import org.jooq.SelectSeekStep1;
import javax.annotation.Resource;
import org.springframework.util.StringUtils;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.enums.StatusEnum;
import com.ke.bella.workflow.api.ops.GroupOps;
import com.ke.bella.workflow.db.tables.pojos.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.ke.bella.workflow.db.tables.records.*;
import com.ke.bella.workflow.enums.MemberTypeEnum;
import static com.ke.bella.workflow.db.tables.WecomGroupInfo.WECOM_GROUP_INFO;
import static com.ke.bella.workflow.db.tables.WecomGroupMember.WECOM_GROUP_MEMBER;

@Component
public class WecomGroupInfoRepo implements BaseRepo {

    private static final String WEB_HOOK_URL_PARAMS_KEY = "key";

    @Resource
    private DSLContext db;

    public WecomGroupInfoDB createGroupInfo(GroupOps.GroupAddOp op) {
        WecomGroupInfoRecord rec = WECOM_GROUP_INFO.newRecord();
        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setSpaceCode(op.getSpaceCode());
        rec.setGroupCode(IDGenerator.newWecomGroupCode());

        if(!StringUtils.isEmpty(op.getGroupName())) {
            rec.setGroupName(op.getGroupName());
        }
        rec.setGroupAlias(op.getGroupAlias());
        if(!StringUtils.isEmpty(op.getGroupId())) {
            rec.setGroupId(op.getGroupId());
        }
        if(!StringUtils.isEmpty(op.getChatId())) {
            rec.setChatId(op.getChatId());
        }
        fillCreatorInfo(rec);
        db.insertInto(WECOM_GROUP_INFO).set(rec).execute();

        return rec.into(WecomGroupInfoDB.class);
    }

    public WecomGroupInfoDB updateGroupInfo(GroupOps.GroupUpdateOp op) {
        WecomGroupInfoRecord rec = WECOM_GROUP_INFO.newRecord();
        if(!StringUtils.isEmpty(op.getGroupName())) {
            rec.setGroupName(op.getGroupName());
        }
        if(!StringUtils.isEmpty(op.getGroupAlias())) {
            rec.setGroupAlias(op.getGroupAlias());
        }
        if(!StringUtils.isEmpty(op.getGroupId())) {
            rec.setGroupId(op.getGroupId());
        }
        if(!StringUtils.isEmpty(op.getChatId())) {
            rec.setChatId(op.getChatId());
        }
        fillUpdatorInfo(rec);

        int num = db.update(WECOM_GROUP_INFO)
                .set(rec)
                .where(WECOM_GROUP_INFO.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_INFO.SPACE_CODE.eq(op.getSpaceCode()))
                .and(WECOM_GROUP_INFO.GROUP_CODE.eq(op.getGroupCode()))
                .execute();
        if(num > 0) {
            return db.selectFrom(WECOM_GROUP_INFO)
                    .where(WECOM_GROUP_INFO.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                    .and(WECOM_GROUP_INFO.SPACE_CODE.eq(op.getSpaceCode()))
                    .and(WECOM_GROUP_INFO.GROUP_CODE.eq(op.getGroupCode()))
                    .fetchOne()
                    .into(WecomGroupInfoDB.class);
        }

        return new WecomGroupInfoDB();
    }

    public Boolean deleteGroupInfo(GroupOps.GroupDeleteOp op) {
        WecomGroupInfoRecord rec = WECOM_GROUP_INFO.newRecord();
        rec.setStatus(StatusEnum.del.getCode());
        fillUpdatorInfo(rec);

        int num = db.update(WECOM_GROUP_INFO)
                .set(rec)
                .where(WECOM_GROUP_INFO.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_INFO.SPACE_CODE.eq(op.getSpaceCode()))
                .and(WECOM_GROUP_INFO.GROUP_CODE.in(op.getGroupCodes()))
                .and(WECOM_GROUP_INFO.CUID.eq(BellaContext.getOperator().getUserId()))
                .and(WECOM_GROUP_INFO.STATUS.eq(StatusEnum.ok.getCode()))
                .execute();

        return num > 0;
    }

    public Page<WecomGroupInfoDB> pageGroupInfo(GroupOps.GroupQueryOp op) {
        SelectSeekStep1<WecomGroupInfoRecord, LocalDateTime> sql = db.selectFrom(WECOM_GROUP_INFO)
                .where(WECOM_GROUP_INFO.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_INFO.SPACE_CODE.eq(op.getSpaceCode()))
                .and(CollectionUtils.isEmpty(op.getGroupCodes()) ? DSL.noCondition() : WECOM_GROUP_INFO.GROUP_CODE.in(op.getGroupCodes()))
                .and(StringUtils.isEmpty(op.getGroupName()) ? DSL.noCondition() : WECOM_GROUP_INFO.GROUP_NAME.like("%" + op.getGroupName() + "%"))
                .and(StringUtils.isEmpty(op.getGroupAlias()) ? DSL.noCondition() : WECOM_GROUP_INFO.GROUP_ALIAS.like("%" + op.getGroupAlias() + "%"))
                .and(WECOM_GROUP_INFO.STATUS.eq(StatusEnum.ok.getCode()))
                .orderBy(WECOM_GROUP_INFO.MTIME.desc());

        return queryPage(db, sql, op.getPage(), op.getPageSize(), WecomGroupInfoDB.class);
    }

    public WecomGroupMemberDB createGroupMemberInfo(GroupOps.GroupMemberOp op) {
        WecomGroupMemberRecord rec = WECOM_GROUP_MEMBER.newRecord();
        if(!StringUtils.isEmpty(op.getUserCode())) {
            rec.setUserCode(op.getUserCode());
        }
        if(!StringUtils.isEmpty(op.getName())) {
            rec.setName(op.getName());
        }
        if(!StringUtils.isEmpty(op.getRobotId())) {
            rec.setRobotId(op.getRobotId());
        }
        if(!StringUtils.isEmpty(op.getRobotWebhook())) {
            rec.setRobotWebhook(op.getRobotWebhook());
            rec.setRobotId(HttpUtils.getQueryParamValue(op.getRobotWebhook(), WEB_HOOK_URL_PARAMS_KEY));
        }
        rec.setType(op.getType());
        WecomGroupMemberDB wecomGroupMember = queryGroupMemberForMember(op);
        if(Objects.nonNull(wecomGroupMember)) {
            throw new IllegalArgumentException("该成员已存在");
        }
        rec.setTenantId(BellaContext.getOperator().getTenantId());
        rec.setSpaceCode(op.getSpaceCode());
        rec.setGroupCode(op.getGroupCode());
        fillCreatorInfo(rec);
        db.insertInto(WECOM_GROUP_MEMBER).set(rec).execute();

        return rec.into(WecomGroupMemberDB.class);
    }

    public WecomGroupMemberDB updateGroupMemberInfo(GroupOps.GroupMemberOp op) {
        WecomGroupMemberRecord rec = WECOM_GROUP_MEMBER.newRecord();
        if(!StringUtils.isEmpty(op.getUserCode())) {
            rec.setUserCode(op.getUserCode());
        }
        if(!StringUtils.isEmpty(op.getName())) {
            rec.setName(op.getName());
        }
        if(!StringUtils.isEmpty(op.getRobotId())) {
            rec.setRobotId(op.getRobotId());
        }
        if(!StringUtils.isEmpty(op.getRobotWebhook())) {
            rec.setRobotWebhook(op.getRobotWebhook());
            rec.setRobotId(HttpUtils.getQueryParamValue(op.getRobotWebhook(), WEB_HOOK_URL_PARAMS_KEY));
        }
        rec.setType(op.getType());
        WecomGroupMemberDB wecomGroupMember = queryGroupMemberForMember(op);
        if(Objects.isNull(wecomGroupMember)) {
            throw new IllegalArgumentException("该成员不存在");
        }
        Condition condition = DSL.noCondition();
        if(MemberTypeEnum.VIRTUAL_NUMBER.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_ID.eq(op.getRobotId());
        }
        if(MemberTypeEnum.ROBOT.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_WEBHOOK.eq(op.getRobotWebhook());
        }
        if(MemberTypeEnum.REAL_USER.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.USER_CODE.eq(op.getUserCode());
        }
        fillUpdatorInfo(rec);
        int num = db.update(WECOM_GROUP_MEMBER).set(rec)
                .where(WECOM_GROUP_MEMBER.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_MEMBER.SPACE_CODE.eq(op.getSpaceCode()))
                .and(WECOM_GROUP_MEMBER.GROUP_CODE.eq(op.getGroupCode()))
                .and(WECOM_GROUP_MEMBER.TYPE.eq(op.getType()))
                .and(condition)
                .and(WECOM_GROUP_MEMBER.STATUS.eq(StatusEnum.ok.getCode()))
                .execute();
        if(num > 0) {
            return db.selectFrom(WECOM_GROUP_MEMBER)
                    .where(WECOM_GROUP_MEMBER.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                    .and(WECOM_GROUP_MEMBER.SPACE_CODE.eq(op.getSpaceCode()))
                    .and(WECOM_GROUP_MEMBER.GROUP_CODE.eq(op.getGroupCode()))
                    .and(WECOM_GROUP_MEMBER.TYPE.eq(op.getType()))
                    .and(WECOM_GROUP_MEMBER.STATUS.eq(StatusEnum.ok.getCode()))
                    .and(condition)
                    .fetchOne()
                    .into(WecomGroupMemberDB.class);
        }

        return new WecomGroupMemberDB();
    }

    public WecomGroupMemberDB queryGroupMemberForMember(GroupOps.GroupMemberOp op) {
        Condition condition = DSL.noCondition();
        if(MemberTypeEnum.VIRTUAL_NUMBER.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_ID.eq(op.getRobotId());
        }
        if(MemberTypeEnum.ROBOT.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_WEBHOOK.eq(op.getRobotWebhook());
        }
        if(MemberTypeEnum.REAL_USER.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.USER_CODE.eq(op.getUserCode());
        }

        return db.selectFrom(WECOM_GROUP_MEMBER)
                .where(WECOM_GROUP_MEMBER.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_MEMBER.SPACE_CODE.eq(op.getSpaceCode()))
                .and(WECOM_GROUP_MEMBER.GROUP_CODE.eq(op.getGroupCode()))
                .and(WECOM_GROUP_MEMBER.TYPE.eq(op.getType()))
                .and(condition)
                .and(WECOM_GROUP_MEMBER.STATUS.eq(StatusEnum.ok.getCode()))
                .fetchOneInto(WecomGroupMemberDB.class);
    }

    public Boolean deleteGroupMemberInfo(GroupOps.GroupMemberOp op) {
        WecomGroupMemberRecord rec = WECOM_GROUP_MEMBER.newRecord();
        rec.setStatus(StatusEnum.del.getCode());
        fillUpdatorInfo(rec);
        Condition condition = DSL.noCondition();
        if(MemberTypeEnum.VIRTUAL_NUMBER.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_ID.eq(op.getRobotId());
        }
        if(MemberTypeEnum.ROBOT.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_WEBHOOK.eq(op.getRobotWebhook());
        }
        if(MemberTypeEnum.REAL_USER.getCode().equals(op.getType())) {
            condition = WECOM_GROUP_MEMBER.USER_CODE.eq(op.getUserCode());
        }
        int num = db.update(WECOM_GROUP_MEMBER).set(rec)
                .where(WECOM_GROUP_MEMBER.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_MEMBER.SPACE_CODE.eq(op.getSpaceCode()))
                .and(WECOM_GROUP_MEMBER.GROUP_CODE.eq(op.getGroupCode()))
                .and(WECOM_GROUP_MEMBER.TYPE.eq(op.getType()))
                .and(condition)
                .and(WECOM_GROUP_MEMBER.STATUS.eq(StatusEnum.ok.getCode()))
                .execute();

        return num > 0;
    }

    public Page<WecomGroupMemberDB> pageGroupMemberInfo(GroupOps.GroupMemberQueryOp op) {
        Condition condition = DSL.noCondition();
        if(!StringUtils.isEmpty(op.getRobotId())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_ID.eq(op.getRobotId());
        }
        if(!StringUtils.isEmpty(op.getRobotWebhook())) {
            condition = WECOM_GROUP_MEMBER.ROBOT_WEBHOOK.eq(op.getRobotWebhook());
        }
        if(!StringUtils.isEmpty(op.getUserCode())) {
            condition = WECOM_GROUP_MEMBER.USER_CODE.eq(op.getUserCode());
        }

        SelectSeekStep1<WecomGroupMemberRecord, LocalDateTime> sql = db.selectFrom(WECOM_GROUP_MEMBER)
                .where(WECOM_GROUP_MEMBER.TENANT_ID.eq(BellaContext.getOperator().getTenantId()))
                .and(WECOM_GROUP_MEMBER.SPACE_CODE.eq(op.getSpaceCode()))
                .and(WECOM_GROUP_MEMBER.GROUP_CODE.eq(op.getGroupCode()))
                .and(!op.getType().equals(MemberTypeEnum.UN_KNOW.getCode())
                        ? WECOM_GROUP_MEMBER.TYPE.eq(op.getType())
                        : DSL.noCondition())
                .and(condition)
                .and(WECOM_GROUP_MEMBER.STATUS.eq(StatusEnum.ok.getCode()))
                .orderBy(WECOM_GROUP_MEMBER.MTIME.desc());

        return queryPage(db, sql, op.getPage(), op.getPageSize(), WecomGroupMemberDB.class);
    }
}
