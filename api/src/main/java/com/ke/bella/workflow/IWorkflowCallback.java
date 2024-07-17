package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.content.DeltaContent;
import com.theokanning.openai.assistants.message.content.Text;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

public interface IWorkflowCallback {

    @Getter
    @Setter
    @SuperBuilder
    public static class ProgressData {
        int progress;
        String object;
        Object data;

        public interface ObjectType {
            String MESSAGE = "message";
            String DELTA_CONTENT = "message.delta";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Delta {
        String role;
        List<DeltaContent> content;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (DeltaContent dc : content) {
                sb.append(dc.getText().getValue());
            }
            return sb.toString();
        }

        public static List<DeltaContent> fromText(String... contents) {
            List<DeltaContent> ret = new ArrayList<>();
            for (String text : contents) {
                ret.add(new DeltaContent(0, "text", new Text(text, null), null, null));
            }
            return ret;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        String role;
        List<MessageContent> content;

        public static List<MessageContent> fromText(String... contents) {
            List<MessageContent> ret = new ArrayList<>();
            for (String text : contents) {
                MessageContent mc = new MessageContent();
                mc.setType("text");
                mc.setText(new Text(text, null));
                ret.add(mc);
            }
            return ret;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class File {
        @JsonAlias("related_id")
        String fileId;
        String filename;
        String extension;
        String mimeType;
        String type;
    }

    void onWorkflowRunStarted(WorkflowContext context);

    void onWorkflowRunSucceeded(WorkflowContext context);

    void onWorkflowRunSuspended(WorkflowContext context);

    void onWorkflowRunResumed(WorkflowContext context);

    void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t);

    void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data);

    void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t);

    void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowIterationNext(WorkflowContext context, String nodeId, String nodeRunId, int index);

    void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId);
}
