# Wait 节点定时自动恢复功能使用说明

## 功能概述

支持在 Groovy/Code 节点返回 wait 状态时，通过 `resumeAfterMinutes` 参数实现延迟自动恢复的能力。系统会在指定时间后自动向节点的 callback URL 发起 HTTP POST 请求，触发工作流恢复执行。

## 核心实现

### 1. 在 Groovy 节点中使用

```groovy
// 返回 waiting 状态，并指定 5 分钟后自动恢复
return NodeRunResult.builder()
    .status(NodeRunResult.Status.waiting)
    .resumeAfterMinutes(5L)  // 5分钟后自动恢复
    .build()
```

### 2. 参数说明

- `resumeAfterMinutes`: Long 类型，表示多少分钟后自动恢复
- 最小值为 1 分钟（系统会自动校验并调整）
- 如果不设置此参数，则保持原有的纯 callback 模式

### 3. 工作流程

```
1. Groovy 节点返回 waiting 状态 + resumeAfterMinutes
   ↓
2. BaseNode.populateResumeInfo() 处理：
   - 校验时间参数（最小1分钟）
   - 生成 triggerId (TRG-SCHD_RESUME-{uuid})
   - 保存到状态变量
   ↓
3. WorkflowRunCallback.onWorkflowNodeRunWaited() 回调：
   - 读取 resumeAfterMinutes、triggerId、callbackUrl
   - 调用 WorkflowService.scheduleWorkflowNodeAutoResume()
   ↓
4. WorkflowRepo 持久化：
   - 插入/更新 workflow_scheduling 表
   - trigger_type = "SCHD_RESUME"
   - trigger_next_time = now + resumeAfterMinutes
   - inputs 字段存储 callbackUrl
   ↓
5. WorkflowSchedulingTriggerHelper 定时扫描（每分钟）：
   - 扫描到期的 SCHD_RESUME 任务
   - 发起 HTTP POST 到 callbackUrl
   - 调用 finishWorkflowNodeAutoResume() 完成任务
   ↓
6. 节点恢复执行
```

## 数据库表结构

使用既有的 `workflow_scheduling` 表：

```sql
CREATE TABLE `workflow_scheduling` (
    `trigger_id` varchar(64) NOT NULL,
    `trigger_type` varchar(16) NOT NULL DEFAULT 'SCHD',  -- 新增 'SCHD_RESUME' 类型
    `trigger_next_time` datetime NOT NULL,
    `inputs` LONGTEXT NOT NULL,  -- SCHD_RESUME 类型时存储 callbackUrl
    `running_status` varchar(32) NOT NULL DEFAULT 'init',
    `status` int NOT NULL DEFAULT 0,
    -- 其他字段...
);
```

## 示例场景

### 场景 1: 轮询外部接口状态

```groovy
// 调用外部接口查询任务状态
def response = httpClient.get("https://api.example.com/task/${taskId}")
def status = sys.fromJson(response).status

if (status == "processing") {
    // 任务仍在处理中，5分钟后自动重试
    return NodeRunResult.builder()
        .status(NodeRunResult.Status.waiting)
        .resumeAfterMinutes(5L)
        .processData([lastCheckTime: new Date().toString()])
        .build()
} else {
    // 任务完成
    return NodeRunResult.builder()
        .status(NodeRunResult.Status.succeeded)
        .outputs([result: status])
        .build()
}
```

### 场景 2: 延迟执行

```groovy
// 需要区分第一次进入和恢复进入
if (!self.isResuming()) {
    // 第一次进入:发送通知后,等待30分钟再继续执行后续流程
    sendNotification(user)

    return NodeRunResult.builder()
        .status(NodeRunResult.Status.waiting)
        .resumeAfterMinutes(30L)
        .processData([notificationSent: true])
        .build()
} else {
    // 恢复进入:直接继续执行
    return [delayCompleted: true]
}
```

### 场景 3: 指数退避重试

```groovy
def retryCount = context.getVariable(nodeId, "retryCount") ?: 0
def maxRetries = 5

try {
    // 尝试执行操作
    def result = performOperation()
    
    return [result: result]
        
} catch (Exception e) {
    if (retryCount < maxRetries) {
        // 指数退避: 1, 2, 4, 8, 16 分钟
        def delayMinutes = Math.pow(2, retryCount) as Long
        context.putVariable(nodeId, "retryCount", retryCount + 1)
        
        return NodeRunResult.builder()
            .status(NodeRunResult.Status.waiting)
            .resumeAfterMinutes(delayMinutes)
            .processData([
                error: e.message, 
                retryCount: retryCount + 1,
                nextRetryIn: delayMinutes
            ])
            .build()
    } else {
        // 超过最大重试次数，失败
        return NodeRunResult.builder()
            .status(NodeRunResult.Status.failed)
            .error(new Exception("Max retries exceeded: ${e.message}"))
            .build()
    }
}
```

## 注意事项

1. **最小延迟时间**: 系统强制最小延迟为 1 分钟
2. **调度精度**: 由于扫描频率为每分钟一次，实际触发时间可能有 ±1 分钟的误差
3. **重复触发**: 同一个 triggerId 如果被多次调用 `scheduleWorkflowNodeAutoResume()`，会使用 UPSERT 更新触发时间
4. **任务清理**: 触发后任务会被标记为 finished 并停用，不会重复执行
5. **兼容性**: 如果不设置 `resumeAfterMinutes`，节点行为与之前完全一致
6. **用户隔离**: SCHD_RESUME 类型的任务不会出现在用户的调度任务列表中（`pageWorkflowScheduling`、`listWorkflowSchedulingWithWorkflow` 已过滤）


## 与既有 callback 模式对比

| 特性 | 纯 callback 模式 | resumeAfterMinutes 模式 |
|------|-----------------|------------------------|
| 使用场景 | 外部系统主动回调 | 定时轮询/延迟执行 |
| 恢复方式 | 外部 HTTP POST | 系统自动 HTTP POST |
| 实现复杂度 | 外部需实现回调 | 只需设置参数 |
| 精确度 | 实时 | ±1 分钟 |
| 持久化 | 内存（10分钟超时） | 数据库持久化 |

## 兼容性说明

本功能向后兼容，不影响现有代码：
- 未设置 `resumeAfterMinutes` 的节点行为不变
- 既有的 callback 模式正常工作
- 可以同时使用两种模式（外部也可以主动回调）
