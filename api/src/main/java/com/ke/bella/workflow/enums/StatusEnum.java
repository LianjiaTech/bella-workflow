package com.ke.bella.workflow.enums;

import lombok.Getter;

@Getter
public enum StatusEnum {

    ok("正常", 0),

    del("已删除", -1);

    private final String desc;

    private final Integer code;

    StatusEnum(String desc, Integer code) {
        this.desc = desc;
        this.code = code;
    }
}
