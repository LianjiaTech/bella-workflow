import type { CommonNodeType, ModelConfig, ValueSelector } from '@/app/components/workflow/types'
import type {
  RerankingModeEnum,
  WeightedScoreEnum,
} from '@/models/datasets'

export type GenerationConfig = {
  model: ModelConfig
  instruction?: string
}
export type MultipleRetrievalConfig = {
  retrieval_mode: RetrievalMode
  top_k: number
  score_threshold: number | null | undefined
  background: boolean
  imageOCR: boolean
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

export type RagNodeType = CommonNodeType & {
  query_variable_selector: ValueSelector
  dataset_ids: string[]
  generation_config?: GenerationConfig
  multiple_retrieval_config?: MultipleRetrievalConfig
  retrieval_mode: RetrievalMode
}

export enum RetrievalMode {
  semantic = 'semantic',
  fusion = 'fusion',
}
