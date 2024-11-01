package com.ke.bella.workflow.api;

import com.ke.bella.workflow.db.BellaContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/console/api/userInfo")
public class UserInfoController {
    @GetMapping
    public Operator getUserInfo() {
        return BellaContext.getOperator();
    }
}
