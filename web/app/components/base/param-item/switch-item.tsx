'use client'

import type { FC } from 'react'
import React from 'react'
import { useTranslation } from 'react-i18next'
import { RiQuestionLine } from '@remixicon/react'
import Switch from '@/app/components/base/switch'
import Tooltip from '@/app/components/base/tooltip-plus'

type Props = {
  className?: string
  value: boolean
  onChange: (value: boolean) => void
  type?: 'background' | 'imageOCR'
}

const SwitchItem: FC<Props> = ({
  className,
  value,
  onChange,
  type = 'background',
}) => {
  const { t } = useTranslation()

  const getTitleKey = () => {
    if (type === 'imageOCR')
      return 'appDebug.feature.dataSet.retrieval.imageOCR.title'
    return 'appDebug.feature.dataSet.retrieval.background.title'
  }

  const getDescriptionKey = () => {
    if (type === 'imageOCR')
      return 'appDebug.feature.dataSet.retrieval.imageOCR.description'
    return 'appDebug.feature.dataSet.retrieval.background.description'
  }

  return (
    <div className={className}>
      <div className="flex items-center h-8">
        <Switch
          size='md'
          defaultValue={value}
          onChange={async (val) => {
            onChange(val)
          }}
        />
        <span className="mx-1 text-gray-900 text-[13px] leading-[18px] font-medium">
          {t(getTitleKey())}
        </span>
        <Tooltip popupContent={<div className="w-[200px]">{t(getDescriptionKey())}</div>}>
          <RiQuestionLine className='w-[14px] h-[14px] text-gray-400' />
        </Tooltip>
      </div>
    </div>
  )
}

export default React.memo(SwitchItem)
