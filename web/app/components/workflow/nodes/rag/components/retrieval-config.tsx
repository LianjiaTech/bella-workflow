'use client'
import type { FC } from 'react'
import React, { useCallback, useState } from 'react'
import { RiEqualizer2Line } from '@remixicon/react'
import { useTranslation } from 'react-i18next'
import type { MultipleRetrievalConfig } from '../types'
import type { ModelConfig } from '../../../types'
import cn from '@/utils/classnames'
import {
  PortalToFollowElem,
  PortalToFollowElemContent,
  PortalToFollowElemTrigger,
} from '@/app/components/base/portal-to-follow-elem'
import ConfigRetrievalContent from '@/app/components/app/configuration/dataset-config/params-config/config-content'
import { DATASET_DEFAULT } from '@/config'
import {
  useModelListAndDefaultModelAndCurrentProviderAndModel,
} from '@/app/components/header/account-setting/model-provider-page/hooks'
import { ModelTypeEnum } from '@/app/components/header/account-setting/model-provider-page/declarations'
import Button from '@/app/components/base/button'
import type { DatasetConfigs } from '@/models/debug'
import type { DataSet } from '@/models/datasets'

type Props = {
  payload: {
    multiple_retrieval_config?: MultipleRetrievalConfig
  }
  onMultipleRetrievalConfigChange: (config: MultipleRetrievalConfig) => void
  singleRetrievalModelConfig?: ModelConfig
  onSingleRetrievalModelChange?: (config: ModelConfig) => void
  onSingleRetrievalModelParamsChange?: (config: ModelConfig) => void
  readonly?: boolean
  openFromProps?: boolean
  onOpenFromPropsChange?: (openFromProps: boolean) => void
  selectedDatasets: DataSet[]
}

const RetrievalConfig: FC<Props> = ({
  payload,
  onMultipleRetrievalConfigChange,
  readonly,
  openFromProps,
  onOpenFromPropsChange,
  selectedDatasets,
}) => {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const mergedOpen = openFromProps !== undefined ? openFromProps : open

  const handleOpen = useCallback((newOpen: boolean) => {
    setOpen(newOpen)
    onOpenFromPropsChange?.(newOpen)
  }, [onOpenFromPropsChange])

  const {
    defaultModel: rerankDefaultModel,
  } = useModelListAndDefaultModelAndCurrentProviderAndModel(ModelTypeEnum.rerank)

  const { multiple_retrieval_config } = payload
  const handleChange = useCallback((configs: DatasetConfigs) => {
    onMultipleRetrievalConfigChange({
      retrieval_mode: configs.retrieval_mode ? configs.retrieval_mode : DATASET_DEFAULT.retrieval_mode,
      top_k: configs.top_k,
      score_threshold: configs.score_threshold_enabled ? (configs.score_threshold || DATASET_DEFAULT.score_threshold) : null,
      background: configs.background ?? true,
      imageOCR: configs.imageOCR ?? false,
      reranking_model: (!configs.reranking_model?.reranking_provider_name
        ? {
          provider: rerankDefaultModel?.provider?.provider || '',
          model: rerankDefaultModel?.model || '',
        }
        : {
          provider: configs.reranking_model?.reranking_provider_name,
          model: configs.reranking_model?.reranking_model_name,
        }),
      reranking_mode: configs.reranking_mode,
      weights: configs.weights as any,
      reranking_enable: configs.reranking_enable,
    })
  }, [onMultipleRetrievalConfigChange, rerankDefaultModel?.provider?.provider, rerankDefaultModel?.model])

  return (
    <PortalToFollowElem
      open={mergedOpen}
      onOpenChange={handleOpen}
      placement='bottom-end'
      offset={{
        crossAxis: -2,
      }}
    >
      <PortalToFollowElemTrigger
        onClick={() => {
          if (readonly)
            return
          handleOpen(!mergedOpen)
        }}
      >
        <Button
          variant='ghost'
          size='small'
          disabled={readonly}
          className={cn(open && 'bg-components-button-ghost-bg-hover')}
        >
          <RiEqualizer2Line className='mr-1 w-3.5 h-3.5'/>
          {t('dataset.retrievalSettings')}
        </Button>
      </PortalToFollowElemTrigger>
      <PortalToFollowElemContent style={{ zIndex: 1001 }}>
        <div className='w-[404px] pt-3 pb-4 px-4 shadow-xl  rounded-2xl border border-gray-200  bg-white'>
          <ConfigRetrievalContent
            datasetConfigs={
              {
                retrieval_mode: multiple_retrieval_config?.retrieval_mode || DATASET_DEFAULT.retrieval_mode,
                reranking_model: multiple_retrieval_config?.reranking_model?.provider
                  ? {
                    reranking_provider_name: multiple_retrieval_config.reranking_model?.provider,
                    reranking_model_name: multiple_retrieval_config.reranking_model?.model,
                  }
                  : {
                    reranking_provider_name: '',
                    reranking_model_name: '',
                  },
                top_k: multiple_retrieval_config?.top_k || DATASET_DEFAULT.top_k,
                score_threshold_enabled: !(multiple_retrieval_config?.score_threshold === undefined || multiple_retrieval_config.score_threshold === null),
                score_threshold: multiple_retrieval_config?.score_threshold,
                background: multiple_retrieval_config?.background ?? true,
                imageOCR: multiple_retrieval_config?.imageOCR ?? false,
                datasets: {
                  datasets: [],
                },
                reranking_mode: multiple_retrieval_config?.reranking_mode,
                weights: multiple_retrieval_config?.weights,
                reranking_enable: multiple_retrieval_config?.reranking_enable,
              }
            }
            onChange={handleChange}
            isInWorkflow
            selectedDatasets={selectedDatasets}
          />
        </div>
      </PortalToFollowElemContent>
    </PortalToFollowElem>
  )
}
export default React.memo(RetrievalConfig)
