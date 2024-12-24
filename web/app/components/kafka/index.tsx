'use client'
import type { FC, SVGProps } from 'react'
import React, { useState } from 'react'
import { usePathname } from 'next/navigation'
import { useTranslation } from 'react-i18next'
import { useDatasourceList } from '../workflow/hooks/use-datasource'
import CreateDatasourceModal from '../datasource/create-datasource-modal'
import List from './list'
import Loading from '@/app/components/base/loading'
import Button from '@/app/components/base/button'

export type ILogsProps = {
  disabled?: boolean
}

const ThreeDotsIcon = ({ className }: SVGProps<SVGElement>) => {
  return <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" className={className ?? ''}>
    <path d="M5 6.5V5M8.93934 7.56066L10 6.5M10.0103 11.5H11.5103" stroke="#374151" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
}

const EmptyElement = () => {
  const { t } = useTranslation()
  const pathname = usePathname()

  return (
    <div className='flex flex-col flex-1 items-center justify-center'>
      <div className='flex flex-col items-center justify-center'>
        <ThreeDotsIcon className='mb-3' />
        <div className='text-sm text-gray-500'>{t('datasource.table.empty.title')}</div>
      </div>
    </div>
  )
}

const Kafka: FC<ILogsProps> = ({ disabled = false }) => {
  const { t } = useTranslation()
  const [showCreateModal, setShowCreateModal] = useState(false)
  const { data: kafkaInstances, mutate, isLoading } = useDatasourceList('kafka')

  const addDatasource = () => {
    setShowCreateModal(true)
  }

  const total = kafkaInstances?.length || 0

  return (
    <div className='flex flex-col h-full px-14 pt-2'>
      <div className='flex flex-col py-4 flex-1'>
        <div className='flex flex-row justify-between'>
          <Button disabled={disabled} variant='primary' onClick={addDatasource}>{t('datasource.add')}</Button>
        </div>
        {isLoading
          ? <Loading type='app' />
          : total > 0
            ? <List disabled={disabled} list={kafkaInstances} onRefresh={mutate} />
            : <EmptyElement />
        }
      </div>
      <CreateDatasourceModal
        show={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={() => {
          mutate()
        }}
        type='kafka'
      />
    </div>
  )
}

export default Kafka
