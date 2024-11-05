package com.ke.bella.workflow.api;

import com.ke.bella.workflow.db.BellaContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/console/api/userInfo")
public class UserInfoController {
    private static final Operator EMPTY = Operator.builder().userId(1L)
            .userName("ai-arch")
            .tenantId("")
            .email("ai-arch@example.com").build();
    @GetMapping
    public Operator getUserInfo() {
        Operator operator = BellaContext.getOperator();
        return operator == null ? EMPTY : operator;
    }
}
