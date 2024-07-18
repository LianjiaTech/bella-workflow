import type { FC } from 'react'
import React, { useCallback, useEffect, useState } from 'react'
import produce from 'immer'
import { useTranslation } from 'react-i18next'
import Item from './json-item'
import type { InputVar } from '@/app/components/workflow/types'
// import s from "../../style.module.css";

// JsonEditor
/*
Event：
- onChange
 */
type Props = {
  value: InputVar[]
  onChange?: (value: InputVar[]) => void
}

const JsonEditor: FC<Props> = ({
  value,
  onChange,
}) => {
  const { t } = useTranslation()
  const [payload, setPayload] = useState<InputVar[]>(value)

  useEffect(() => {
    setPayload(value)
  }, [value])

  const handleCheckInputVars = useCallback((index: number, value: InputVar) => {
    const newPayload = produce(payload, (draft: InputVar[]) => {
      draft[index] = value
    })
    setPayload(newPayload)
    if (onChange)
      onChange(newPayload)
  }, [payload, setPayload, onChange])

  return (
    <div className='mt-1'>
      <div className='mt-2 space-y-2'>
        <div className='space-y-2'>
          <div className='flex items-center space-x-1'>
            <div className='w-[298px] leading-8 text-[13px] font-medium text-gray-700'>{t('appDebug.variableConig.varName')}</div>
            <div className=' w-[76px] leading-8 text-[13px] font-medium text-gray-700'>
              {t('appDebug.variableConig.varType')}
            </div>
            <div className='w-[173px] leading-8 text-[13px] font-medium text-gray-700'>
              {t('appDebug.variableConig.describe')}
            </div>
            <div className='w-[60px] leading-8 text-[13px] font-medium text-gray-700'>
              {t('appDebug.variableConig.isNecessary')}
            </div>
            <div className='leading-8 text-[13px] font-medium text-gray-700'>
              {t('appDebug.variableConig.operate')}
            </div>
          </div>
          {payload && payload.map((item, index) => (
            <div key={item.variable} className='flex items-center space-x-1 w-full !mt-0'>
              <Item
                index={index}
                value={item}
                onChange={handleCheckInputVars}
              /></div>
          ))}

        </div>
      </div>
    </div>
  )
}

export default React.memo(JsonEditor)
