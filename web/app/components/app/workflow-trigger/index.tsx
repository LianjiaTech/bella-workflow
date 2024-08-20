'use client'
import type { FC, SVGProps } from 'react'
import React, { useState } from 'react'
import useSWR from 'swr'
import { usePathname } from 'next/navigation'
import { Trans, useTranslation } from 'react-i18next'
import Link from 'next/link'
import List from './list'
import Filter from './filter'
import Loading from '@/app/components/base/loading'
import Button from '@/app/components/base/button'
import { fetchWorkflowTriggers } from '@/service/log'
import type { App, AppMode } from '@/types/app'
import CreateTriggerModal from '@/app/components/app/create-workflow-trigger-modal'

export type ILogsProps = {
  appDetail: App
}

export type QueryParam = {
  triggerType?: string
}

const ThreeDotsIcon = ({ className }: SVGProps<SVGElement>) => {
  return <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" className={className ?? ''}>
    <path d="M5 6.5V5M8.93934 7.56066L10 6.5M10.0103 11.5H11.5103" stroke="#374151" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
}
const EmptyElement: FC<{ appUrl: string }> = ({ appUrl }) => {
  const { t } = useTranslation()
  const pathname = usePathname()
  const pathSegments = pathname.split('/')
  pathSegments.pop()
  return <div className='flex items-center justify-center h-full'>
    <div className='bg-gray-50 w-[560px] h-fit box-border px-5 py-4 rounded-2xl'>
      <span className='text-gray-700 font-semibold'>{t('appLog.table.empty.element.title')}<ThreeDotsIcon className='inline relative -top-3 -left-1.5' /></span>
      <div className='mt-2 text-gray-500 text-sm font-normal'>
        <Trans
          i18nKey="workflow.trigger.table.empty.element.content"
          components={{ shareLink: <Link href={`${pathSegments.join('/')}/overview`} className='text-primary-600' />, testLink: <Link href={appUrl} className='text-primary-600' target='_blank' rel='noopener noreferrer' /> }}
        />
      </div>
    </div>
  </div>
}

const Triggers: FC<ILogsProps> = ({ appDetail }) => {
  const { t } = useTranslation()
  const [showNewTriggerModal, setShowNewTriggerModal] = useState(false)
  const [queryParams, setQueryParams] = useState<QueryParam>({ triggerType: 'SCHD' })

  const query = {
    ...queryParams,
  }

  const getWebAppType = (appType: AppMode) => {
    if (appType !== 'completion' && appType !== 'workflow')
      return 'chat'
    return appType
  }

  const addTrigger = () => {
    setShowNewTriggerModal(true)
  }

  const { data: workflowLogs, mutate } = useSWR({
    url: `/apps/${appDetail.id}/workflow-triggers`,
    params: query,
  }, fetchWorkflowTriggers)

  const total = workflowLogs?.data?.length || 0

  return (
    <div className='flex flex-col h-full'>
      <h1 className='text-md font-semibold text-gray-900'>{t('workflow.trigger.title')}</h1>
      <p className='flex text-sm font-normal text-gray-500'>{t('workflow.trigger.desc')}</p>
      <div className='flex flex-col py-4 flex-1'>
        <div className='flex flex-row justify-between'>
          <Filter queryParams={queryParams} setQueryParams={setQueryParams} />
          <Button variant='primary' onClick={addTrigger}>{t('workflow.trigger.add')}</Button>
        </div>
        {/* workflow log */}
        {workflowLogs === undefined
          ? <Loading type='app' />
          : total > 0
            ? <List logs={workflowLogs} appDetail={appDetail} onRefresh={mutate} />
            : <EmptyElement appUrl={`${appDetail.site.app_base_url}/${getWebAppType(appDetail.mode)}/${appDetail.site.access_token}`} />
        }
      </div>
      <CreateTriggerModal
        workflowId={appDetail.id}
        show={showNewTriggerModal}
        onClose={() => setShowNewTriggerModal(false)}
        onSuccess={() => {
          mutate()
        }}
      />
    </div>
  )
}

export default Triggers
