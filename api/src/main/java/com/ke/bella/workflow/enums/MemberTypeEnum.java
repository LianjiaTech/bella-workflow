package com.ke.bella.workflow.enums;

import lombok.Getter;

@Getter
public enum MemberTypeEnum {

    UN_KNOW("未知", 0),
    VIRTUAL_NUMBER("虚拟账号", 1),
    ROBOT("机器人", 2),
    REAL_USER("真实用户", 3);

    private final String desc;

    private final Integer code;

    MemberTypeEnum(String desc, Integer code) {
        this.desc = desc;
        this.code = code;
    }

    public static MemberTypeEnum getByCode(Integer code) {
        for (MemberTypeEnum item : values()) {
            if(item.code == code) {
                return item;
            }
        }
        return null;
    }
}
