package com.ke.bella.workflow.service;

import javax.annotation.Resource;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.api.ops.GroupOps;
import com.ke.bella.workflow.db.tables.pojos.*;
import org.springframework.stereotype.Component;
import com.ke.bella.workflow.db.repo.WecomGroupInfoRepo;

@Component
public class WecomGroupService {

    @Resource
    private WecomGroupInfoRepo repo;

    public WecomGroupInfoDB createGroupInfo(GroupOps.GroupAddOp op) {
        return repo.createGroupInfo(op);
    }

    public WecomGroupInfoDB updateGroupInfo(GroupOps.GroupUpdateOp op) {
        return repo.updateGroupInfo(op);
    }

    public Boolean deleteGroupInfo(GroupOps.GroupDeleteOp op) {

        return repo.deleteGroupInfo(op);
    }

    public Page<WecomGroupInfoDB> pageGroupInfo(GroupOps.GroupQueryOp op) {
        return repo.pageGroupInfo(op);
    }

    public WecomGroupInfoDB groupInfoByUserId(GroupOps.GroupQueryByUserOp op) {
        return repo.groupInfoByUserId(op);
    }

    public WecomGroupInfoDB groupInfoByGroupId(GroupOps.GroupQueryByGroupOp op) {
        return repo.groupInfoByGroupId(op);
    }

    public WecomGroupMemberDB createGroupMemberInfo(GroupOps.GroupMemberOp op) {

        return repo.createGroupMemberInfo(op);
    }

    public WecomGroupMemberDB updateGroupMemberInfo(GroupOps.GroupMemberOp op) {

        return repo.updateGroupMemberInfo(op);
    }

    public Boolean deleteMemberInfo(GroupOps.GroupMemberOp op) {

        return repo.deleteGroupMemberInfo(op);
    }

    public Page<WecomGroupMemberDB> pageGroupMemberInfo(GroupOps.GroupMemberQueryOp op) {

        return repo.pageGroupMemberInfo(op);
    }
}
