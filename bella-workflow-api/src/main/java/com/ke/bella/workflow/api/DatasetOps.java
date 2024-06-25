package com.ke.bella.workflow.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public class DatasetOps {

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetPage extends Operator {
        @Builder.Default
        int page = 1;

        @Builder.Default
        int limit = 20;

        String name;

        List<String> ids;

        @Builder.Default
        String provider = "vendor";

        String keyword;

        String tagIds;
    }

}
