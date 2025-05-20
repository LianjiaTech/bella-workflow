package com.ke.bella.workflow.tool;

import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.tool.ApiTool.Credentials;
import com.ke.bella.workflow.tool.BellaToolService.BellaTool;
import com.ke.bella.workflow.tool.BellaToolService.BellaToolCredentialsType;
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
        BellaToolCredentialsType credential = BellaToolCredentialsType.from(authType);
        if(credential.equals(BellaToolCredentialsType.None)) {
            return null;
        }
        BellaTool.AuthData authData = tool.getAuthData();
        List<BellaTool.AuthData.Param> params = authData.getParams();
        String key = null;
        String apiKey = null;
        String secret = null;

        for (BellaTool.AuthData.Param param : params) {
            if(param.getKey().equals(credential.getKey())) {
                key = param.getValue();
            } else if(param.getKey().equals(credential.getApiKey())) {
                apiKey = param.getValue();
            } else if(param.getKey().equals(credential.getSecret())) {
                secret = param.getValue();
            }
        }

        key = StringUtils.isEmpty(key) ? "Authorization" : key;
        return new Credentials(credential.getAuthType(), credential.getPrefix(), key, apiKey, secret);
    }
}
