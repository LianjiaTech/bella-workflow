package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.List;

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
            String DELTA_CONTENT = "message.delta";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Delta {
        /**
         * The entity that produced the message. One of user or assistant.
         */
        String role;

        List<DeltaContent> content;

        public static List<DeltaContent> fromText(String... contents) {
            List<DeltaContent> ret = new ArrayList<>();
            for (String text : contents) {
                ret.add(new DeltaContent(0, "text", new Text(text, null), null, null));
            }
            return ret;
        }
    }

    void onWorkflowRunStarted(WorkflowContext context);

    void onWorkflowRunSucceeded(WorkflowContext context);

    void onWorkflowRunSuspended(WorkflowContext context);

    void onWorkflowRunResumed(WorkflowContext context);

    void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t);

    void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, ProgressData data);

    void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId);

    void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t);
}
