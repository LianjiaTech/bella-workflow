import type { FC } from 'react'
import React from 'react'
import { useTranslation } from 'react-i18next'
import { RiQuestionLine } from '@remixicon/react'
import ReactMarkdown from 'react-markdown'
import Select from '@/app/components/base/select'
import Tooltip from '@/app/components/base/tooltip-plus'

type Props = {
  value: string
  onChange: (mode: string) => void
}

const RetrievalModeItem: FC<Props> = ({
  value,
  onChange,
}) => {
  const { t } = useTranslation()

  const options = [
    { value: 'semantic', name: t('appDebug.feature.dataSet.retrieval.mode.semantic') },
    { value: 'fusion', name: t('appDebug.feature.dataSet.retrieval.mode.fusion') },
  ]

  return (
    <div className='flex items-center justify-between'>
      <div className='grow'>
        <div className='mx-1 text-gray-900 text-[13px] leading-[18px] font-medium'>
          {t('appDebug.feature.dataSet.retrieval.mode.title')}
          <Tooltip popupContent={<ReactMarkdown>{t('appDebug.feature.dataSet.retrieval.mode.description')}</ReactMarkdown>}>
            <RiQuestionLine className='w-[14px] h-[14px] text-gray-400 ml-1' />
          </Tooltip>
        </div>
      </div>
      <Select
        className='w-[160px]'
        items={options}
        defaultValue={value}
        onSelect={item => onChange(item.value as string)}
      />
    </div>
  )
}

export default React.memo(RetrievalModeItem)
