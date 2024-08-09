package com.ke.bella.workflow.api;

import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.ke.bella.workflow.enums.ResultCodeEnum;

@Data
@SuperBuilder
@NoArgsConstructor
@ToString
public class BellaResponse<T> {
    private int code;
    private String message;
    private long timestamp;
    private T data;
    private String stacktrace;

	public BellaResponse<T> fail(ResultCodeEnum resultCode, String message) {
		this.code = resultCode.getCode();
		this.message = message;
		this.timestamp = System.currentTimeMillis();
		return this;
	}

	public BellaResponse<T> success(T data) {
		this.code = ResultCodeEnum.SUCCESS.getCode();
		this.message = ResultCodeEnum.SUCCESS.getMessage();
		this.timestamp = System.currentTimeMillis();
		this.data = data;
		return this;
	}
}
