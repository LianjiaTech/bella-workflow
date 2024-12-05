package com.ke.bella.workflow;

import com.ke.bella.workflow.node.BaseNode;

public class WorkflowNodeRunException extends RuntimeException {

    private static final long serialVersionUID = -7357832765932432578L;

    private final String message;

    public WorkflowNodeRunException(BaseNode<?> node, Throwable cause) {
        super(cause);
        this.message = createMessage(node, cause);

    }

    @Override
    public String getMessage() {
        return message;
    }

    public static WorkflowNodeRunException from(BaseNode<?> node, Throwable cause) {
        if(cause instanceof WorkflowNodeRunException) {
            return (WorkflowNodeRunException) cause;
        }
        return new WorkflowNodeRunException(node, cause);
    }

    private String createMessage(BaseNode<?> node, Throwable cause) {
        return String.format("节点`%s`执行异常：%s", node.getMeta().getTitle(), cause.getMessage());
    }

}
