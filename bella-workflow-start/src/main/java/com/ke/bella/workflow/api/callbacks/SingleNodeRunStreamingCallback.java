package com.ke.bella.workflow.api.callbacks;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.WorkflowCallbackAdaptor;

public class SingleNodeRunStreamingCallback extends WorkflowCallbackAdaptor {
    final SseEmitter emitter;

    public SingleNodeRunStreamingCallback(SseEmitter emitter) {
        this.emitter = emitter;
    }
}
