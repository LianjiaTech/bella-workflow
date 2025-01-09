package com.ke.bella.workflow.trigger;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.api.WechatCallBackBody;
import com.ke.bella.workflow.db.repo.WorkflowTriggerRepo;
import com.ke.bella.workflow.db.tables.pojos.WorkflowWebotTriggerDB;
import com.ke.bella.workflow.utils.HttpUtils;

@Component
public class WebotTriggerRunner {

    @Autowired
    WorkflowTriggerRepo repo;

    @Autowired
    WorkflowSchedulingTriggerHelper helper;

    public void recv(WechatCallBackBody body) {
        String robotId = HttpUtils.getQueryParamValue(body.getWebhookUrl(), "key");

        List<WorkflowWebotTriggerDB> ts = repo.listAllActiveWebotTriggers(robotId);
        List<WorkflowWebotTriggerDB> ts2 = ts.stream().filter(t -> canTrigger(t, body))
                .collect(Collectors.toList());
        helper.tryWebotTrigger(ts2, body);
    }

    boolean canTrigger(WorkflowWebotTriggerDB db, WechatCallBackBody event) {
        if(StringUtils.hasText(db.getChatId()) && !db.getChatId().equals(event.getChatId())) {
            return false;
        }

        if(StringUtils.hasText(db.getExpression())) {
            return ExpressionHelper.canTrigger(db.getExpressionType(), db.getId().toString(), db.getExpression(), event);
        }

        return true;
    }
}
