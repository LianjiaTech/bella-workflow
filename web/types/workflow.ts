import type { Viewport } from 'reactflow'
import type {
  BlockEnum,
  Edge,
  EnvironmentVariable,
  HistoryWorkflowVersion, Node,
} from '@/app/components/workflow/types'

export type NodeTracing = {
  id: string
  index: number
  predecessor_node_id: string
  node_id: string
  node_type: BlockEnum
  title: string
  inputs: any
  process_data: any
  outputs?: any
  status: string
  error?: string
  elapsed_time: number
  execution_metadata: {
    total_tokens: number
    total_price: number
    currency: string
    steps_boundary: number[]
  }
  metadata: {
    iterator_length: number
  }
  created_at: number
  created_by: {
    id: string
    name: string
    email: string
  }
  finished_at: number
  extras?: any
  expand?: boolean // for UI
  details?: NodeTracing[][] // iteration detail
}

export type FetchWorkflowDraftResponse = {
  id: string
  graph: {
    nodes: Node[]
    edges: Edge[]
    viewport?: Viewport
  }
  features?: any
  created_at: number
  created_by: {
    id: string
    name: string
    email: string
  }
  hash: string
  updated_at: number
  tool_published: boolean
  environment_variables?: EnvironmentVariable[]
}

export type NodeTracingListResponse = {
  data: NodeTracing[]
}

export type WorkflowStartedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    workflow_id: string
    sequence_number: number
    created_at: number
  }
}

export type WorkflowFinishedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    workflow_id: string
    status: string
    outputs: any
    error: string
    elapsed_time: number
    total_tokens: number
    total_steps: number
    created_at: number
    created_by: {
      id: string
      name: string
      email: string
    }
    finished_at: number
  }
}

export type NodeStartedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    node_id: string
    node_type: string
    index: number
    predecessor_node_id?: string
    inputs: any
    created_at: number
    extras?: any
    iteration_id?: string
  }
}

export type NodeFinishedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    node_id: string
    node_type: string
    index: number
    predecessor_node_id?: string
    inputs: any
    process_data: any
    outputs: any
    status: string
    error: string
    elapsed_time: number
    execution_metadata: {
      total_tokens: number
      total_price: number
      currency: string
    }
    created_at: number
    iteration_id?: string
  }
}

export type IterationStartedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    node_id: string
    metadata: {
      iterator_length: number
    }
    created_at: number
    extras?: any
  }
}

export type IterationNextedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    node_id: string
    index: number
    output: any
    extras?: any
    created_at: number
  }
}

export type IterationFinishedResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    id: string
    node_id: string
    outputs: any
    extras?: any
    status: string
    created_at: number
    error: string
  }
}

export type TextChunkResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    text: string
  }
}

export type TextReplaceResponse = {
  task_id: string
  workflow_run_id: string
  event: string
  data: {
    text: string
  }
}

export type WorkflowRunHistory = {
  id: string
  sequence_number: number
  version: string
  conversation_id?: string
  message_id?: string
  graph: {
    nodes: Node[]
    edges: Edge[]
    viewport?: Viewport
  }
  inputs: Record<string, string>
  status: string
  outputs: Record<string, any>
  error?: string
  elapsed_time: number
  total_tokens: number
  total_steps: number
  created_at: number
  finished_at: number
  created_by_account: {
    id: string
    name: string
    email: string
  }
}
export type WorkflowRunHistoryResponse = {
  data: WorkflowRunHistory[]
}

export type WorkflowVersionHistoryResponse = {
  data: HistoryWorkflowVersion[]
  limit: number
  page: number
  has_more: boolean
  total: number
}

export type ChatRunHistoryResponse = {
  data: WorkflowRunHistory[]
}

export type NodesDefaultConfigsResponse = {
  type: string
  config: any
}[]

export type WorkflowTriggerDetail = {
  triggerId: string
  triggerType: String
  name: string
  desc: string
  datasource: string
  condition: string
  status: string
  workflowId: string
  inputs: string
}
export type WorkflowOutputs = {
  tenantId: string
  workflowId: string
  workflowRunId: string
  status: string
  outputs: any
  error: string
}
export type Datasource = {
  datasourceId: string
  datasourceType: string
  name: string
  config: object
}

export type CustomApiDetail = {
  id: number
  host: string
  path: string
  operationId: string
  status: number
}

export type CustomApiListResponse = {
  data: CustomApiDetail[]
}

export type CustomDomain = {
  domain: string
  desc: string
}

export type UserInfoResponse = {
  userId: number
  userName: string
  email: string
  tenantId: string
  spaceCode: string
}
