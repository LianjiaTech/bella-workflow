'use client'
import type { FC, SVGProps } from 'react'
import React, { useState } from 'react'
import { usePathname } from 'next/navigation'
import { Trans, useTranslation } from 'react-i18next'
import Link from 'next/link'
import { useDatasourceList } from '../workflow/hooks/use-datasource'
import List from './list'
import Loading from '@/app/components/base/loading'
import Button from '@/app/components/base/button'
import CreateDatasourceModal from '@/app/components/datasource/create-datasource-modal'

export type ILogsProps = {
  disabled?: boolean
}

const ThreeDotsIcon = ({ className }: SVGProps<SVGElement>) => {
  return <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" className={className ?? ''}>
    <path d="M5 6.5V5M8.93934 7.56066L10 6.5M10.0103 11.5H11.5103" stroke="#374151" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
}
const EmptyElement: FC<{}> = () => {
  const { t } = useTranslation()
  const pathname = usePathname()
  const pathSegments = pathname.split('/')
  pathSegments.pop()
  return <div className='flex items-center justify-center h-full'>
    <div className='bg-gray-50 w-[560px] h-fit box-border px-5 py-4 rounded-2xl'>
      <span className='text-gray-700 font-semibold'>{t('datasource.table.empty.title')}<ThreeDotsIcon className='inline relative -top-3 -left-1.5' /></span>
      <div className='mt-2 text-gray-500 text-sm font-normal'>
        <Trans
          i18nKey="workflow.trigger.table.empty.element.content"
          components={{ shareLink: <Link href={`${pathSegments.join('/')}/overview`} className='text-primary-600' /> }}
        />
      </div>
    </div>
  </div>
}

const Triggers: FC<ILogsProps> = ({ disabled = false }) => {
  const { t } = useTranslation()
  const [showCreateModal, setShowCreateModal] = useState(false)
  const { data: rdbs, mutate, isLoading } = useDatasourceList('rdb')

  const addDatasource = () => {
    setShowCreateModal(true)
  }

  const total = rdbs?.length || 0

  return (
    <div className='flex flex-col h-full px-14 pt-2'>
      <div className='flex flex-col py-4 flex-1'>
        <div className='flex flex-row justify-between'>
          <Button disabled={disabled} variant='primary' onClick={addDatasource}>{t('datasource.add')}</Button>
        </div>
        {isLoading
          ? <Loading type='app' />
          : total > 0
            ? <List disabled={disabled} list={rdbs} onRefresh={mutate} />
            : <EmptyElement />
        }
      </div>
      <CreateDatasourceModal
        type={'rdb'}
        show={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={() => {
          mutate()
        }}
      />
    </div>
  )
}

export default Triggers
