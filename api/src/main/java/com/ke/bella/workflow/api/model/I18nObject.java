package com.ke.bella.workflow.api.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class I18nObject {
    private String zh_Hans;
    private String en_US;
}
