'use client'
import type { FC } from 'react'
import React from 'react'
import { useTranslation } from 'react-i18next'
import type { QueryParam } from './index'
import Chip from '@/app/components/base/chip'
import Input from '@/app/components/base/input'

type IFilterProps = {
  queryParams: QueryParam
  setQueryParams: (v: QueryParam) => void
}

const Filter: FC<IFilterProps> = ({ queryParams, setQueryParams }: IFilterProps) => {
  const { t } = useTranslation()
  return (
    <div className='flex flex-row flex-wrap gap-2 mb-2'>
      <Chip
        value={queryParams.status || 'all'}
        onSelect={(item) => {
          setQueryParams({ ...queryParams, status: item.value as string })
        }}
        onClear={() => setQueryParams({ ...queryParams, status: 'all' })}
        items={[{ value: 'all', name: 'All' },
          { value: 'succeeded', name: 'Success' },
          { value: 'failed', name: 'Fail' },
          { value: 'stopped', name: 'Stop' },
        ]}
      />
      <Input
        wrapperClassName='w-[200px]'
        showLeftIcon
        showClearIcon
        value={queryParams.workflowRunId}
        placeholder={'输入workflow_run_id'}
        onChange={(e) => {
          setQueryParams({ ...queryParams, workflowRunId: e.target.value })
        }}
        onClear={() => setQueryParams({ ...queryParams, workflowRunId: '' })}
      />
      <Input
        wrapperClassName='w-[200px]'
        showLeftIcon
        showClearIcon
        value={queryParams.userId}
        placeholder={'输入ucid'}
        pattern="[0-9]*"
        onKeyDown={(e) => {
          if (!/^\d$/.test(e.key)
              && !['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab'].includes(e.key))
            e.preventDefault()
        }}
        onChange={(e) => {
          const value = e.target.value
          if (value === '') {
            setQueryParams({ ...queryParams, userId: undefined })
            return
          }
          const numValue = Number(value)
          if (!isNaN(numValue))
            setQueryParams({ ...queryParams, userId: numValue })
        }}
        onClear={() => setQueryParams({ ...queryParams, userId: undefined })}
      />
      {/* <Input
        wrapperClassName='w-[200px]'
        showLeftIcon
        showClearIcon
        value={queryParams.keyword}
        placeholder={t('common.operation.search')!}
        onChange={(e) => {
          setQueryParams({ ...queryParams, keyword: e.target.value })
        }}
        onClear={() => setQueryParams({ ...queryParams, keyword: '' })}
      /> */}
    </div>
  )
}

export default Filter
