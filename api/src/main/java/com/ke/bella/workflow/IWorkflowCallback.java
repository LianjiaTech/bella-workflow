
package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ke.bella.workflow.utils.JsonUtils;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.content.DeltaContent;
import com.theokanning.openai.assistants.message.content.Text;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
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
            String LOG = "log";
        }

        public interface EventType {
            String MESSAGE_DELTA = "message.delta";
            String MESSAGE_COMPLETED = "message.completed";
        }

        public static ProgressData log(String log) {
            return ProgressData.builder().object(ObjectType.LOG)
                    .data(log)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Delta {
        String messageId;
        String role;
        String name;
        List<DeltaContentX> content;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (DeltaContentX dc : content) {
                if(dc.getText() != null) {
                    sb.append(dc.getText().getValue());
                }

                ImageDelta imgd = dc.getImageDelta();
                if(imgd != null) {
                    sb.append(JsonUtils.toJson(imgd));
                }

            }
            return sb.toString();
        }

        public static List<DeltaContentX> fromText(String... contents) {
            List<DeltaContentX> ret = new ArrayList<>();
            for (String text : contents) {
                ret.add(DeltaContentX.from(new DeltaContent(0, "text", new Text(text, null), null, null)));
            }
            return ret;
        }

        public static List<DeltaContentX> fromImageDelta(String id, String url, int progress) {
            DeltaContentX c = DeltaContentX.builder()
                    .imageDelta(ImageDelta.builder()
                            .id(id)
                            .url(url)
                            .progress(progress)
                            .build())
                    .build();
            c.setType("image_delta");
            return Arrays.asList(c);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    public class ImageDelta {
        @NonNull
        private String url;

        private String id;

        private int progress;

        public ImageDelta(String url) {
            this.url = url;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeltaContentX extends DeltaContent {
        ImageDelta imageDelta;

        public static DeltaContentX from(DeltaContent o) {
            DeltaContentX c = new DeltaContentX();
            c.setType(o.getType());
            c.setImageUrl(o.getImageUrl());
            c.setImageFile(o.getImageFile());
            c.setText(o.getText());
            return c;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        String messageId;
        String role;
        String name;
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

    void onWorkflowRunStopped(WorkflowContext context);

    void onWorkflowRunResumed(WorkflowContext context);

    void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t);

    void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId, String nodeRunId, ProgressData data);

    void onWorkflowNodeRunWaited(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId, String nodeRunId);

    void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String nodeRunId, String error, Throwable t);

    void onWorkflowIterationStarted(WorkflowContext context, String nodeId, String nodeRunId, int index);

    void onWorkflowIterationCompleted(WorkflowContext context, String nodeId, String nodeRunId, int index);
}
