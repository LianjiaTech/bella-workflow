package com.ke.bella.workflow.tool;

import org.noear.solon.ai.mcp.McpChannel;
import org.noear.solon.ai.mcp.client.McpClientProvider;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class McpTool implements ITool, AutoCloseable {

    private final BellaToolService.McpServerConfig mcpServerConfig;
    private final String toolName;
    private final AtomicReference<McpClientProvider> providerRef = new AtomicReference<>();

    public McpTool(BellaToolService.McpServerConfig mcpServerConfig, String toolName) {
        Assert.notNull(mcpServerConfig, "McpServerConfig must not be null");
        Assert.hasText(toolName, "ToolName must not be empty");

        this.mcpServerConfig = mcpServerConfig;
        this.toolName = toolName;
    }

    @Override
    public String execute(Map<String, Object> params) {
        Objects.requireNonNull(params, "Params must not be null");

        try {
            McpClientProvider provider = getOrCreateProvider();
			return provider.callToolAsText(toolName, params).getContent();
        } catch (Exception e) {
            throw new McpToolException("Failed to execute MCP tool: " + toolName, e);
        }
    }

    private McpClientProvider getOrCreateProvider() {
        McpClientProvider provider = providerRef.get();
        if (provider == null) {
            provider = createMcpClientProvider();
            if (!providerRef.compareAndSet(null, provider)) {
                try {
                    provider.close();
                } catch (Exception ignored) {

                }
                provider = providerRef.get();
            }
        }
        return provider;
    }

    private McpClientProvider createMcpClientProvider() {
        return McpClientProvider.builder()
                .channel(McpChannel.STREAMABLE)
                .apiUrl(mcpServerConfig.getUrl())
                .headerSet(mcpServerConfig.getHeaders())
                .build();
    }

    @PreDestroy
    @Override
    public void close() {
        McpClientProvider provider = providerRef.get();
        if (provider != null) {
            try {
                provider.close();
            } catch (Exception ignored) {
            }
        }
    }
}

class McpToolException extends RuntimeException {
    public McpToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
