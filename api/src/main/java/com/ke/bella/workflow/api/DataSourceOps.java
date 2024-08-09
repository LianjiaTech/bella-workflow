package com.ke.bella.workflow.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public class DataSourceOps {

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KafkaDataSourceAdd extends Operator {
        String server;
        String topic;
        String name;
        String msgSchema;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KafkaDataSourceRm extends Operator {
        String datasourceID;
    }
}
