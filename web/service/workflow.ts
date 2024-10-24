import type { Fetcher } from 'swr'
import { get, post } from './base'
import type { CommonResponse } from '@/models/common'
import type {
  ChatRunHistoryResponse,
  FetchWorkflowDraftResponse,
  NodesDefaultConfigsResponse,
  WorkflowRunHistoryResponse,
  WorkflowTriggerDetail,
  WorkflowVersionHistoryResponse,
} from '@/types/workflow'
import type { BlockEnum, HistoryWorkflowVersion } from '@/app/components/workflow/types'

export const fetchWorkflowDraft = (url: string) => {
  return get(url, {}, { silent: true }) as Promise<FetchWorkflowDraftResponse>
}

export const syncWorkflowDraft = ({ url, params }: { url: string; params: Pick<FetchWorkflowDraftResponse, 'graph' | 'features' | 'environment_variables'> }) => {
  return post<CommonResponse & { updated_at: number; hash: string }>(url, { body: params }, { silent: true })
}

export const fetchNodesDefaultConfigs: Fetcher<NodesDefaultConfigsResponse, string> = (url) => {
  return get<NodesDefaultConfigsResponse>(url)
}

export const fetchWorkflowRunHistory: Fetcher<WorkflowRunHistoryResponse, string> = (url) => {
  return get<WorkflowRunHistoryResponse>(url)
}

export const fetchWorkflowVersionHistory: Fetcher<WorkflowVersionHistoryResponse, string> = (url) => {
  return get<WorkflowVersionHistoryResponse>(url)
}

export const activateWorkflowVersion: Fetcher<HistoryWorkflowVersion, { workflowId: string; version: number }> = ({ workflowId, version }) => {
  return post<HistoryWorkflowVersion>(`apps/${workflowId}/workflow-versions/activate`, { body: { workflowId, version } })
}

export const deactivateWorkflowVersion: Fetcher<HistoryWorkflowVersion, { workflowId: string }> = ({ workflowId }) => {
  return post<HistoryWorkflowVersion>(`apps/${workflowId}/workflow-versions/deactivate`, { body: { workflowId } })
}

export const fetchDefaultWorkflowVersion: Fetcher<HistoryWorkflowVersion, string> = (url) => {
  return get<HistoryWorkflowVersion>(url)
}

export const fetcChatRunHistory: Fetcher<ChatRunHistoryResponse, string> = (url) => {
  return get<ChatRunHistoryResponse>(url)
}

export const singleNodeRun = (appId: string, nodeId: string, params: object) => {
  return post(`apps/${appId}/workflows/draft/nodes/${nodeId}/run`, { body: params })
}

export const getIterationSingleNodeRunUrl = (isChatFlow: boolean, appId: string, nodeId: string) => {
  return `apps/${appId}/${isChatFlow ? 'advanced-chat/' : ''}workflows/draft/iteration/nodes/${nodeId}/run`
}

export const publishWorkflow = (url: string) => {
  return post<CommonResponse & { created_at: number }>(url)
}

export const fetchPublishedWorkflow: Fetcher<FetchWorkflowDraftResponse, string> = (url) => {
  return get<FetchWorkflowDraftResponse>(url)
}

export const stopWorkflowRun = (url: string) => {
  return post<CommonResponse>(url)
}

export const fetchNodeDefault = (appId: string, blockType: BlockEnum, query = {}) => {
  return get(`apps/${appId}/workflows/default-workflow-block-configs/${blockType}`, {
    params: { q: JSON.stringify(query) },
  })
}

export const updateWorkflowDraftFromDSL = (appId: string, data: string) => {
  return post<FetchWorkflowDraftResponse>(`apps/${appId}/workflows/draft/import`, { body: data }, { bodyStringify: false })
}

export const createSchedulingTrigger: Fetcher<WorkflowTriggerDetail, { name: string; desc?: string; expression: string; inputs: string; workflowId: string; triggerType: string; datasourceId: string }> = ({ name, desc, expression, inputs, workflowId, triggerType, datasourceId }) => {
  return post<WorkflowTriggerDetail>(`apps/${workflowId}/trigger/create`, { body: { name, desc, expression, inputs, workflowId, triggerType, datasourceId } })
}

export const activateTrigger: Fetcher<WorkflowTriggerDetail, { workflowId: string; triggerId: string; triggerType: string }> = ({ workflowId, triggerId, triggerType }) => {
  return post<WorkflowTriggerDetail>(`apps/${workflowId}/trigger/activate`, { body: { triggerId, triggerType } })
}

export const deactivateTrigger: Fetcher<WorkflowTriggerDetail, { workflowId: string; triggerId: string; triggerType: string }> = ({ workflowId, triggerId, triggerType }) => {
  return post<WorkflowTriggerDetail>(`apps/${workflowId}/trigger/deactivate`, { body: { triggerId, triggerType } })
}
