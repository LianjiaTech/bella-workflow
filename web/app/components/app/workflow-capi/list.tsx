'use client'
import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useContext } from 'use-context-selector'
import s from './style.module.css'
import Loading from '@/app/components/base/loading'
import type { App } from '@/types/app'
import { ToastContext } from '@/app/components/base/toast'
import type { CustomApiDetail, CustomApiListResponse } from '@/types/workflow'

type ILogs = {
  logs?: CustomApiListResponse
  appDetail?: App
  onRefresh: () => void
}

const WorkflowAppLogList: FC<ILogs> = ({ logs, appDetail, onRefresh }) => {
  const { t } = useTranslation()
  const { notify } = useContext(ToastContext)
  const [currentLog, setCurrentLog] = useState<CustomApiDetail | undefined>()

  if (!logs || !appDetail)
    return <Loading />

  return (
    <div className='overflow-x-auto'>
      <table className={`w-full min-w-[440px] border-collapse border-0 text-sm mt-3 ${s.logTable}`}>
        <thead className="h-8 !pl-3 py-2 leading-[18px] border-b border-gray-200 text-xs text-gray-500 font-medium">
          <tr>
            <td className='whitespace-nowrap'>{t('workflow.customApi.table.header.host')}</td>
            <td className='whitespace-nowrap'>{t('workflow.customApi.table.header.path')}</td>
            {/* <td className='whitespace-nowrap'>{t('workflow.customApi.table.header.operationId')}</td> */}
            <td className='whitespace-nowrap'>{t('workflow.customApi.table.header.status')}</td>
            <td className='whitespace-nowrap'>{t('workflow.customApi.table.header.actions')}</td>
          </tr>
        </thead>
        <tbody className="text-gray-700 text-[13px]">
          {logs.data.map((t: CustomApiDetail) => {
            return <tr
              key={t.id}
              className={`border-b border-gray-200 h-8 hover:bg-gray-50 cursor-pointer ${currentLog?.id !== t.id ? '' : 'bg-gray-50'}`}
              onClick={() => {
                setCurrentLog(t)
              }}>
              <td>{t.host}</td>
              <td>{t.path}</td>
              {/* <td>{t.operationId}</td> */}
              <td>{t.status === 0 ? 'active' : 'inactive'}</td>
              <td>
                <div className="flex items-center gap-2">
                  {/* <Button variant="secondary" size="small" onClick={() => { setShowDocModal(true) }}>
                    调用
                  </Button> */}
                </div>
              </td>
            </tr>
          })}
        </tbody>
      </table>
    </div>
  )
}

export default WorkflowAppLogList
