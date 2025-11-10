package com.ke.bella.workflow.tool;

import okhttp3.logging.HttpLoggingInterceptor;
import org.noear.solon.ai.mcp.McpChannel;
import org.noear.solon.ai.mcp.client.McpClientProvider;

import java.util.*;

public class McpTool implements ITool {
	static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

	static {
		logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
	}

	private final BellaToolService.McpServerConfig mcpServerConfig;
	private final String toolName;

	public McpTool(BellaToolService.McpServerConfig mcpServerConfig, String toolName) {
		this.mcpServerConfig = mcpServerConfig;
		this.toolName = toolName;
	}

	@Override
	public String execute(Map<String, Object> params) {
		McpClientProvider provider = McpClientProvider.builder()
			.channel(McpChannel.STREAMABLE)
			.apiUrl(mcpServerConfig.getUrl())
			.headerSet(mcpServerConfig.getHeaders())
			.build();
		String response = null;
		try {
			response = provider.callToolAsText(toolName, params).getContent();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return response;
	}
}
