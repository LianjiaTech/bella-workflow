package com.ke.bella.workflow.trigger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.googlecode.aviator.AviatorEvaluator;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    boolean canTrigger(WorkflowWebotTriggerDB db, WechatCallBackBody event) {
        if(StringUtils.hasText(db.getChatId()) && !db.getChatId().equals(event.getChatId())) {
            return false;
        }

        if(StringUtils.hasText(db.getExpression())) {
            Map env = new HashMap();
            env.put("event", event);

            Object res = AviatorEvaluator.execute(db.getId().toString(), db.getExpression(), env, true);
            return res instanceof Boolean && (Boolean) res;
        }

        return true;
    }

    public static void validate(String expression) {
        AviatorEvaluator.compile(expression);
    }
}
