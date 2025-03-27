'use client'

import { memo } from 'react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import TopKItem from '@/app/components/base/param-item/top-k-item'
import ScoreThresholdItem from '@/app/components/base/param-item/score-threshold-item'
import RetrievalModeItem from '@/app/components/base/param-item/retrieval-mode-item'
import SwitchItem from '@/app/components/base/param-item/switch-item'
import type {
  DatasetConfigs,
} from '@/models/debug'
import type {
  DataSet,
} from '@/models/datasets'
import { useSelectedDatasetsMode } from '@/app/components/workflow/nodes/knowledge-retrieval/hooks'
import { RetrievalMode } from '@/app/components/workflow/nodes/rag/types'

type Props = {
  datasetConfigs: DatasetConfigs
  onChange: (configs: DatasetConfigs) => void
  isInWorkflow?: boolean
  selectedDatasets?: DataSet[]
}

const ConfigContent: FC<Props> = ({
  datasetConfigs,
  onChange,
  selectedDatasets = [],
}) => {
  const { t } = useTranslation()
  const selectedDatasetsMode = useSelectedDatasetsMode(selectedDatasets)

  const handleParamChange = (key: string, value: number) => {
    if (key === 'top_k') {
      onChange({
        ...datasetConfigs,
        top_k: value,
      })
    }
    else if (key === 'score_threshold') {
      onChange({
        ...datasetConfigs,
        score_threshold: value,
      })
    }
  }

  const handleRetrievalModeChange = (value: string) => {
    onChange({
      ...datasetConfigs,
      retrieval_mode: value === RetrievalMode.semantic ? RetrievalMode.semantic : RetrievalMode.fusion,
    })
  }

  const handleSwitch = (key: string, enable: boolean) => {
    if (key === 'top_k')
      return

    onChange({
      ...datasetConfigs,
      score_threshold_enabled: enable,
    })
  }

  const handleBackgroundChange = (enable: boolean) => {
    onChange({
      ...datasetConfigs,
      background: enable,
    })
  }

  const handleImageOCRChange = (enable: boolean) => {
    onChange({
      ...datasetConfigs,
      imageOCR: enable,
    })
  }

  const showWeightedScorePanel = true
  return (
    <div>
      <div className='system-xl-semibold text-text-primary'>{t('dataset.retrievalSettings')}</div>
      <div className='mb-2 mt-4 h-[1px] bg-divider-subtle'></div>
      {
        selectedDatasetsMode.inconsistentEmbeddingModel
        && (
          <div className='mt-4 system-xs-regular text-text-warning'>
            {t('dataset.inconsistentEmbeddingModelTip')}
          </div>
        )
      }
      {
        selectedDatasetsMode.mixtureHighQualityAndEconomic
        && (
          <div className='mt-4 system-xs-regular text-text-warning'>
            {t('dataset.mixtureHighQualityAndEconomicTip')}
          </div>
        )
      }
      {
        showWeightedScorePanel
        && (
          <div className='mt-2 space-y-4'>
            <RetrievalModeItem
              value={datasetConfigs.retrieval_mode}
              onChange={handleRetrievalModeChange}
            />
            <TopKItem
              value={datasetConfigs.top_k}
              onChange={handleParamChange}
              enable={true}
            />
            <ScoreThresholdItem
              value={datasetConfigs.score_threshold as number}
              onChange={handleParamChange}
              enable={datasetConfigs.score_threshold_enabled}
              hasSwitch={true}
              onSwitchChange={handleSwitch}
            />
            <SwitchItem
              value={datasetConfigs.background}
              onChange={handleBackgroundChange}
              type="background"
            />
            <SwitchItem
              value={datasetConfigs.imageOCR}
              onChange={handleImageOCRChange}
              type="imageOCR"
            />
          </div>
        )
      }
    </div >
  )
}
export default memo(ConfigContent)
