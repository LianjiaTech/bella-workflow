'use client'
import type { FC } from 'react'
import React from 'react'
import { useTranslation } from 'react-i18next'

import type { QueryParam } from './index'
import { SimpleSelect } from '@/app/components/base/select'

type IFilterProps = {
  queryParams: QueryParam
  setQueryParams: (v: QueryParam) => void
}

const Filter: FC<IFilterProps> = ({ queryParams, setQueryParams }: IFilterProps) => {
  const { t } = useTranslation()
  return (
    <div className='flex flex-row flex-wrap gap-y-2 gap-x-4 items-center mb-4 text-gray-900 text-base'>
      <div className="relative rounded-md">
        <SimpleSelect
          defaultValue={'SCHD'}
          className='!min-w-[150px]'
          onSelect={
            (item) => {
              setQueryParams({ ...queryParams, triggerType: item.value as string })
            }
          }
          items={[{ value: 'SCHD', name: '定时任务' },
            { value: 'KFKA', name: 'kafka事件' },
            { value: 'WBOT', name: '企微群机器人' },
          ]}
        />
      </div>
    </div>
  )
}

export default Filter
