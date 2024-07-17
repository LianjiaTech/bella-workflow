package com.ke.bella.workflow.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ke.bella.workflow.db.BellaContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.ke.bella.workflow.api.DatasetOps;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.utils.HttpUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
public class DatasetService {

    private static final String X_BELLA_TENANT_ID = "X-BELLA-TENANT-ID";
    private static final String BELLA_WORKFLOW_TENANT_ID = "bella-workflow";
    private static final String X_BELLA_OPERATOR_ID = "X-BELLA-OPERATOR-ID";
    private static final String X_BELLA_OPERATOR_NAME = "X-BELLA-OPERATOR-NAME";
    @Value("${bella.knowledge-file.search}")
    private String bellaKnowledgeFileSearchUrl;

    private static Dataset transfer(BellaResp.Page.KnowledgeFile bellaFile) {
        return Dataset.builder()
                .id(bellaFile.getFileId())
                .name(bellaFile.getFileName())
                // fixme：此处和bella保持一致，无脑返回可用状态
                .embedding_available(true)
                .permission(BellaResp.Page.KnowledgeFile.Visibility.PRIVATE.desc.equals(bellaFile.getVisibility())
                        ? Dataset.Permission.only_me.name()
                        : Dataset.Permission.all_team_members.name())
                .build();
    }

    public Page<Dataset> pageDataset(DatasetOps.DatasetPage op) {
        Map<String, String> header = null;
        try {
            header = ImmutableMap.of(X_BELLA_TENANT_ID, BELLA_WORKFLOW_TENANT_ID, X_BELLA_OPERATOR_ID,
                    String.valueOf(BellaContext.getOperator().getUserId()), X_BELLA_OPERATOR_NAME,
                    URLEncoder.encode(BellaContext.getOperator().getUserName(),
                            StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        // fixme: 暂时只支持pageNo、pageSize、ids
        BellaKnowledgeFileSearchReq searchReq = BellaKnowledgeFileSearchReq.builder()
                .pageNo(op.getPage())
                .pageSize(op.getLimit())
                .fileIds(op.getIds())
                .build();
        BellaResp bellaResp = HttpUtils.postJson(header, bellaKnowledgeFileSearchUrl, JsonUtils.toJson(searchReq), new TypeReference<BellaResp>() {
        });
        if(!"0".equals(bellaResp.getErrno())) {
            throw new IllegalStateException("Failed to get dataset list from Bella Knowledge Repository");
        }
        return postHandleBellaResp(bellaResp, op.getPage(), op.getLimit());
    }

    private Page<Dataset> postHandleBellaResp(BellaResp bellaResp, int page, int pageSize) {
        Page<Dataset> result = Page.from(page, pageSize);
        BellaResp.Page bellaPage = bellaResp.getData();
        if(Objects.isNull(bellaPage) || CollectionUtils.isEmpty(bellaPage.getList())) {
            return result.list(Collections.emptyList());
        }
        List<BellaResp.Page.KnowledgeFile> bellaFiles = bellaPage.getList();
        List<Dataset> entities = bellaFiles.stream().map(DatasetService::transfer).collect(Collectors.toList());
        result.list(entities);
        result.total(bellaPage.getTotal());
        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Dataset {
        private String id;
        private String name;
        private String description;
        private String permission;
        /**
         * 影响前端是否可选中
         */
        private boolean embedding_available;

        public enum Permission {
            all_team_members,
            only_me
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BellaKnowledgeFileSearchReq {
        private int pageNo;
        private int pageSize;
        private List<String> fileIds;
        @Builder.Default
        private String sort = "ALL";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BellaResp {
        private String errno;
        private String errMsg;
        private Page data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Page {
            List<KnowledgeFile> list;
            Integer fileNum;
            Integer htmlNum;
            Integer pageNum;
            Integer pageSize;
            Integer total;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class KnowledgeFile {
                private String fileId;
                private String fileName;
                private String fileUrl;
                private String status;
                private String visibility;

                @AllArgsConstructor
                public enum Visibility {
                    PRIVATE("private"),
                    PUBLIC("public");

                    private String desc;
                }

                public enum Status {
                    upload_success,
                    embedding_success,
                }
            }
        }
    }
}
