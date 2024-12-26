'use client'
import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { RiRestartLine, RiShutDownLine } from '@remixicon/react'
import { useContext } from 'use-context-selector'
import Button from '../base/button'
import s from './style.module.css'
import Loading from '@/app/components/base/loading'
import { ToastContext } from '@/app/components/base/toast'
import { activeDatasource, deactiveDatasource } from '@/service/common'

type RedisInstance = {
  datasourceId: string
  host: string
  port: number
  user: string
  db: number
  status: number
}

type ILogs = {
  list?: RedisInstance[]
  onRefresh: () => void
  disabled: boolean
}

const RedisList: FC<ILogs> = ({ list, onRefresh, disabled }) => {
  const { t } = useTranslation()
  const { notify } = useContext(ToastContext)
  const [currentItem, setCurrentItem] = useState<RedisInstance | undefined>()

  if (!list)
    return <Loading />

  const toggleDs = async (datasourceId: string, status: number) => {
    if (status === 0) {
      await deactiveDatasource({ datasourceId, type: 'redis' })
      notify({ type: 'success', message: t('datasource.table.toggle.deactivated') })
    }
    else {
      await activeDatasource({ datasourceId, type: 'redis' })
      notify({ type: 'success', message: t('datasource.table.toggle.activated') })
    }
    onRefresh()
  }

  return (
    <div className='overflow-x-auto'>
      <table className={`w-full min-w-[440px] border-collapse border-0 text-sm mt-3 ${s.logTable}`}>
        <thead className="h-8 !pl-3 py-2 leading-[18px] border-b border-gray-200 text-xs text-gray-500 font-medium">
          <tr>
            <td className='whitespace-nowrap'>{t('datasource.table.datasourceId')}</td>
            <td className='whitespace-nowrap'>{t('datasource.table.host')}</td>
            <td className='whitespace-nowrap'>{t('datasource.table.port')}</td>
            <td className='whitespace-nowrap'>{t('datasource.table.user')}</td>
            <td className='whitespace-nowrap'>{t('datasource.table.db')}</td>
            <td className='whitespace-nowrap'>{t('datasource.table.status')}</td>
            <td className='whitespace-nowrap'>{t('datasource.table.actions')}</td>
          </tr>
        </thead>
        <tbody className="text-gray-700 text-[13px]">
          {list.map((instance) => {
            return <tr
              key={instance.datasourceId}
              className={`border-b border-gray-200 h-8 hover:bg-gray-50 cursor-pointer ${currentItem?.datasourceId !== instance.datasourceId ? '' : 'bg-gray-50'}`}
              onClick={() => {
                setCurrentItem(instance)
              }}>
              <td>{instance.datasourceId}</td>
              <td>{instance.host}</td>
              <td>{instance.port}</td>
              <td>{instance.user}</td>
              <td>{instance.db}</td>
              <td>{instance.status === 0 ? 'active' : 'deactive'}</td>
              <td>
                <div className="flex items-center gap-2">
                  <Button disabled={disabled} variant="secondary" size="small" onClick={(e) => {
                    e.stopPropagation()
                    toggleDs(instance.datasourceId, instance.status)
                  }}>
                    {instance.status === 0 ? <RiShutDownLine className="h-4 w-4" /> : <RiRestartLine className="h-4 w-4" />}
                  </Button>
                </div>
              </td>
            </tr>
          })}
        </tbody>
      </table>
    </div>
  )
}

export default RedisList
