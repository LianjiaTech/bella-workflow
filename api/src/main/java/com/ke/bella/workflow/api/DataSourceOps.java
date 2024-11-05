package com.ke.bella.workflow.api;

import java.util.Map;
import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
    public static class DataSourceOp extends Operator {
        String datasourceId;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RdbDataSourceAdd extends Operator {
        String host;
        int port;
        String db;
        String user;
        String password;
        String dbType;

        @Builder.Default
        Map<String, String> params = new HashMap<>();
    }

}
