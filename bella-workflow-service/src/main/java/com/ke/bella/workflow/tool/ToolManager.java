package com.ke.bella.workflow.tool;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.ke.bella.workflow.tool.ApiTool.Credentials;
import com.ke.bella.workflow.tool.BellaToolService.BellaTool;
import com.ke.bella.workflow.tool.BellaToolService.BellaToolCredentials;
import com.ke.bella.workflow.utils.OpenapiUtil;

public class ToolManager {
    public static ApiTool getApiTool(String toolName) {
        List<BellaTool> tools = BellaToolService.getTool(toolName);
        if(CollectionUtils.isEmpty(tools)) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        return transfer(tools.get(0));
    }

    public static ApiTool transfer(BellaTool tool) {
        Credentials credentials = getCredentials(tool);
        ApiTool.ToolBundle toolBundle = OpenapiUtil.extractToolBundleFromOpenapi(tool.getToolSchema(), tool.getToolName());
        return new ApiTool(toolBundle, credentials);
    }

    private static Credentials getCredentials(BellaTool tool) {
        Integer authType = tool.getAuthType();
        BellaToolCredentials credential = BellaToolCredentials.from(authType);
        if(credential.equals(BellaToolCredentials.None)) {
            return null;
        }
        BellaTool.AuthData authData = tool.getAuthData();
        List<BellaTool.AuthData.Param> params = authData.getParams();
        String key = null;
        String value = null;
        for (BellaTool.AuthData.Param param : params) {
            if(param.getKey().equals(credential.getKeyName())) {
                key = param.getValue();
            } else if(param.getKey().equals(credential.getValueName())) {
                value = param.getValue();
            }
        }
        value = credential.getPrefix() + value;
        return new Credentials(key, value);
    }
}
