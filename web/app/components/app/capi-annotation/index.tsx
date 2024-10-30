'use client'
import type { FC } from 'react'
import React from 'react'
import cn from '@/utils/classnames'
import WorkflowCApi from '@/app/components/app/workflow-capi'
import Loading from '@/app/components/base/loading'
import { useStore as useAppStore } from '@/app/components/app/store'

const CApiAnnotation: FC = () => {
  const appDetail = useAppStore(state => state.appDetail)

  if (!appDetail) {
    return (
      <div className='flex h-full items-center justify-center bg-white'>
        <Loading />
      </div>
    )
  }

  return (
    <div className='pt-4 px-6 h-full flex flex-col'>
      <div className={cn('grow')}>
        <WorkflowCApi appDetail={appDetail} />
      </div>
    </div>
  )
}
export default React.memo(CApiAnnotation)
