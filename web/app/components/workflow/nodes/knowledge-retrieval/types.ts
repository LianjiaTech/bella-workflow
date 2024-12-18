import type { CommonNodeType, ModelConfig, ValueSelector } from '@/app/components/workflow/types'
import type { RETRIEVE_TYPE } from '@/types/app'
import type {
  RerankingModeEnum,
  WeightedScoreEnum,
} from '@/models/datasets'
import type { RetrievalMode } from '@/app/components/workflow/nodes/rag/types'

export type MultipleRetrievalConfig = {
  retrieval_mode: RetrievalMode
  top_k: number
  background: boolean
  score_threshold: number | null | undefined
  reranking_model?: {
    provider: string
    model: string
  }
  reranking_mode?: RerankingModeEnum
  weights?: {
    weight_type: WeightedScoreEnum
    vector_setting: {
      vector_weight: number
      embedding_provider_name: string
      embedding_model_name: string
    }
    keyword_setting: {
      keyword_weight: number
    }
  }
  reranking_enable?: boolean
}

export type SingleRetrievalConfig = {
  model: ModelConfig
}

export type KnowledgeRetrievalNodeType = CommonNodeType & {
  query_variable_selector: ValueSelector
  dataset_ids: string[]
  retrieval_mode: RETRIEVE_TYPE
  multiple_retrieval_config?: MultipleRetrievalConfig
  single_retrieval_config?: SingleRetrievalConfig
}
