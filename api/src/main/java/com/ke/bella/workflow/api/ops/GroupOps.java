package com.ke.bella.workflow.api.ops;

import lombok.*;
import java.util.List;
import java.util.ArrayList;
import com.ke.bella.workflow.api.Operator;
import io.swagger.annotations.ApiModelProperty;

@Data
public class GroupOps {

    @Getter
    @Setter
    @Data
    public static class GroupAddOp extends Operator {

        @ApiModelProperty(value = "群名字")
        private String groupName = "";

        @ApiModelProperty(value = "群备注", required = true)
        private String groupAlias = "";

        @ApiModelProperty(value = "企微中台ID-虚拟号指定群发消息")
        private String groupId = "";

        @ApiModelProperty(value = "会话Id-群Id-机器人指定群发消息")
        private String chatId = "";

        @ApiModelProperty(value = "空间编码，默认：personal，.......")
        private String spaceCode = "";

        @ApiModelProperty(value = "会话id")
        private String threadId = "";

        @ApiModelProperty(value = "exclusive_ai_assistant：专属AI助理群.......")
        private String scene = "";
    }

    @Getter
    @Setter
    @Data
    public static class GroupUpdateOp extends Operator {

        @ApiModelProperty(value = "群编码-暗号")
        private String groupCode = "";

        @ApiModelProperty(value = "群名字")
        private String groupName = "";

        @ApiModelProperty(value = "群备注", required = true)
        private String groupAlias = "";

        @ApiModelProperty(value = "企微中台ID-虚拟号指定群发消息")
        private String groupId = "";

        @ApiModelProperty(value = "会话Id-群Id-机器人指定群发消息")
        private String chatId = "";

        @ApiModelProperty(value = "会话id")
        private String threadId = "";

        @ApiModelProperty(value = "空间编码，默认：personal，.......")
        private String spaceCode = "";
    }

    @Getter
    @Setter
    @Data
    public static class GroupDeleteOp extends Operator {

        @ApiModelProperty(value = "群编码-暗号")
        private List<String> groupCodes = new ArrayList<>();

        @ApiModelProperty(value = "空间编码，默认：personal，.......")
        private String spaceCode = "";
    }

    @Getter
    @Setter
    @Data
    public static class GroupQueryOp extends Operator {

        @ApiModelProperty(value = "空间编码，默认：personal，.......")
        private String spaceCode = "";

        @ApiModelProperty(value = "群编码-暗号")
        private List<String> groupCodes = new ArrayList<>();

        @ApiModelProperty(value = "群名字")
        private String groupName = "";

        @ApiModelProperty(value = "群备注", required = true)
        private String groupAlias = "";

        @ApiModelProperty(value = "exclusive_ai_assistant：专属AI助理群.......")
        private String scene = "";

        @ApiModelProperty(value = "页码", required = true)
        private int page = 1;

        @ApiModelProperty(value = "每页数量", required = true)
        private int pageSize = 20;
    }

    @Getter
    @Setter
    @Data
    public static class GroupQueryByGroupOp extends Operator {

        @ApiModelProperty(value = "群ID")
        private String groupId = "";

        @ApiModelProperty(value = "exclusive_ai_assistant：专属AI助理群.......")
        private String scene = "";
    }

    @Getter
    @Setter
    @Data
    public static class GroupQueryByUserOp extends Operator {

        @ApiModelProperty(value = "空间编码，默认：personal，.......")
        private String spaceCode = "";

        @ApiModelProperty(value = "exclusive_ai_assistant：专属AI助理群.......")
        private String scene = "";
    }

    @Getter
    @Setter
    @Data
    public static class GroupMemberOp extends Operator {

        @ApiModelProperty(value = "空间编码，默认：personal，.......")
        private String spaceCode = "";

        @ApiModelProperty(value = "群编码-暗号")
        private String groupCode = "";

        @ApiModelProperty(value = "群成员系统号")
        private String userCode = "";

        @ApiModelProperty(value = "机器人ID")
        private String robotId = "";

        @ApiModelProperty(value = "外部机器人ID")
        private String robotOuterId = "";

        @ApiModelProperty(value = "名称")
        private String name = "";

        @ApiModelProperty(value = "机器人钩子地址")
        private String robotWebhook = "";

        @ApiModelProperty(value = "成员类型（0:未知,1:虚拟账号,2:机器人,3:真实用户）")
        private Integer type = 0;
    }

    @Getter
    @Setter
    @Data
    public static class GroupMemberQueryOp extends GroupMemberOp {

        @ApiModelProperty(value = "页码", required = true)
        private int page = 1;

        @ApiModelProperty(value = "每页数量", required = true)
        private int pageSize = 20;
    }
}
