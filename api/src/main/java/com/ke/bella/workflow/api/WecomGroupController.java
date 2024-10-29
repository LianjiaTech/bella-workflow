package com.ke.bella.workflow.api;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import io.swagger.annotations.Api;
import org.springframework.util.Assert;
import com.ke.bella.workflow.db.repo.Page;
import org.springframework.util.StringUtils;
import com.ke.bella.workflow.api.ops.GroupOps;
import org.springframework.web.bind.annotation.*;
import com.ke.bella.workflow.enums.MemberTypeEnum;
import com.ke.bella.workflow.service.WecomGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import com.ke.bella.workflow.db.tables.pojos.WecomGroupInfoDB;
import com.ke.bella.workflow.db.tables.pojos.WecomGroupMemberDB;

@Slf4j
@RestController
@Api(tags = "群信息管理")
@RequestMapping("/v1/wecom/group")
public class WecomGroupController {

    @Autowired
    WecomGroupService groupService;

    @PostMapping("/create")
    public WecomGroupInfoDB createGroupInfo(@RequestBody GroupOps.GroupAddOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.getGroupAlias(), "groupAlias不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName不能为空");

        return groupService.createGroupInfo(op);
    }

    @PostMapping("/update")
    public WecomGroupInfoDB updateGroupInfo(@RequestBody GroupOps.GroupUpdateOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode不能为空");
        Assert.hasText(op.getGroupCode(), "groupCode不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName不能为空");

        return groupService.updateGroupInfo(op);
    }

    @PostMapping("/delete")
    public Boolean deleteGroupInfo(@RequestBody GroupOps.GroupDeleteOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode不能为空");
        Assert.notEmpty(op.getGroupCodes(), "groupCodes不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName不能为空");

        return groupService.deleteGroupInfo(op);
    }

    @PostMapping("/page")
    public Page<WecomGroupInfoDB> pageGroupInfo(@RequestBody GroupOps.GroupQueryOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName 不能为空");

        return groupService.pageGroupInfo(op);
    }

    @GetMapping("/userId")
    public WecomGroupInfoDB queryGroupInfoByUserId(GroupOps.GroupQueryByUserOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode不能为空");
        Assert.hasText(op.getScene(), "scene不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName 不能为空");

        return groupService.groupInfoByUserId(op);
    }

    @GetMapping("/groupId")
    public WecomGroupInfoDB queryGroupInfoByGroupId(GroupOps.GroupQueryByGroupOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.getGroupId(), "groupId不能为空");
        Assert.hasText(op.getScene(), "scene不能为空");

        return groupService.groupInfoByGroupId(op);
    }

    @PostMapping("member/create")
    public WecomGroupMemberDB createGroupMember(@RequestBody GroupOps.GroupMemberOp op) {
        Assert.hasText(op.tenantId, "tenantId 不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode 不能为空");
        Assert.hasText(op.getGroupCode(), "groupCode 不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName 不能为空");
        validMemberInfo(op);

        return groupService.createGroupMemberInfo(op);
    }

    @PostMapping("member/update")
    public WecomGroupMemberDB updateGroupMember(@RequestBody GroupOps.GroupMemberOp op) {
        Assert.hasText(op.tenantId, "tenantId 不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode 不能为空");
        Assert.hasText(op.getGroupCode(), "groupCode 不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName 不能为空");
        validMemberInfo(op);

        return groupService.updateGroupMemberInfo(op);
    }

    @PostMapping("member/delete")
    public Boolean deleteGroupMember(@RequestBody GroupOps.GroupMemberOp op) {
        Assert.hasText(op.tenantId, "tenantId 不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode 不能为空");
        Assert.hasText(op.getGroupCode(), "groupCode 不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName 不能为空");
        validMemberInfo(op);

        return groupService.deleteMemberInfo(op);
    }

    @PostMapping("member/page")
    public Page<WecomGroupMemberDB> pageGroupMemberInfo(@RequestBody GroupOps.GroupMemberQueryOp op) {
        Assert.hasText(op.tenantId, "tenantId 不能为空");
        Assert.hasText(op.getSpaceCode(), "spaceCode 不能为空");
        Assert.hasText(op.getGroupCode(), "groupCode 不能为空");
        Assert.notNull(op.getUserId(), "userId 不能为空");
        Assert.hasText(op.getUserName(), "userName 不能为空");

        return groupService.pageGroupMemberInfo(op);
    }

    public void validMemberInfo(GroupOps.GroupMemberOp op) {
        if(Objects.isNull(MemberTypeEnum.getByCode(op.getType()))
                || op.getType().equals(MemberTypeEnum.UN_KNOW.getCode())) {
            throw new IllegalArgumentException(String.format("memberInfo.type=%s 不支持", op.getType()));
        }
        if(MemberTypeEnum.VIRTUAL_NUMBER.getCode().equals(op.getType())
                && StringUtils.isEmpty(op.getRobotId())) {
            throw new IllegalArgumentException("robotId不为空");
        }
        if(MemberTypeEnum.ROBOT.getCode().equals(op.getType())
                && StringUtils.isEmpty(op.getRobotWebhook())) {
            throw new IllegalArgumentException("robotWebhook不为空");
        }
        if(MemberTypeEnum.REAL_USER.getCode().equals(op.getType())
                && StringUtils.isEmpty(op.getUserCode())) {
            throw new IllegalArgumentException("userCode不为空");
        }
    }
}
