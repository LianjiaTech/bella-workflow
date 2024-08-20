'use client'
import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { RiRestartLine, RiShutDownLine } from '@remixicon/react'
import { useContext } from 'use-context-selector'
import Button from '../../base/button'
import s from './style.module.css'
import Loading from '@/app/components/base/loading'
import useBreakpoints from '@/hooks/use-breakpoints'
import type { WorkflowTriggerDetail, WorkflowTriggersResponse } from '@/models/log'
import type { App } from '@/types/app'
import { activateTrigger, deactivateTrigger } from '@/service/workflow'
import { ToastContext } from '@/app/components/base/toast'

type ILogs = {
  logs?: WorkflowTriggersResponse
  appDetail?: App
  onRefresh: () => void
}

const WorkflowAppLogList: FC<ILogs> = ({ logs, appDetail, onRefresh }) => {
  const { t } = useTranslation()
  const { notify } = useContext(ToastContext)
  const media = useBreakpoints()
  const [currentLog, setCurrentLog] = useState<WorkflowTriggerDetail | undefined>()

  if (!logs || !appDetail)
    return <Loading />

  const toggleTrigger = async (triggerId: string, triggerType: string, status: string) => {
    if (status === 'active') {
      await deactivateTrigger({ workflowId: appDetail.id, triggerId, triggerType })
      notify({ type: 'success', message: t('workflow.trigger.toggle.deactivate') })
    }
    else {
      await activateTrigger({ workflowId: appDetail.id, triggerId, triggerType })
      notify({ type: 'success', message: t('workflow.trigger.toggle.activate') })
    }
    onRefresh()
  }

  return (
    <div className='overflow-x-auto'>
      <table className={`w-full min-w-[440px] border-collapse border-0 text-sm mt-3 ${s.logTable}`}>
        <thead className="h-8 !pl-3 py-2 leading-[18px] border-b border-gray-200 text-xs text-gray-500 font-medium">
          <tr>
            <td className='whitespace-nowrap'>{t('workflow.trigger.table.header.id')}</td>
            <td className='whitespace-nowrap'>{t('workflow.trigger.table.header.name')}</td>
            <td className='whitespace-nowrap'>{t('workflow.trigger.table.header.type')}</td>
            <td className='whitespace-nowrap'>{t('workflow.trigger.table.header.expression')}</td>
            <td className='whitespace-nowrap'>{t('workflow.trigger.table.header.status')}</td>
            <td className='whitespace-nowrap'>{t('workflow.trigger.table.header.actions')}</td>
          </tr>
        </thead>
        <tbody className="text-gray-700 text-[13px]">
          {logs.data.map((t: WorkflowTriggerDetail) => {
            return <tr
              key={t.triggerId}
              className={`border-b border-gray-200 h-8 hover:bg-gray-50 cursor-pointer ${currentLog?.triggerId !== t.triggerId ? '' : 'bg-gray-50'}`}
              onClick={() => {
                setCurrentLog(t)
              }}>
              <td>{t.triggerId}</td>
              <td>{t.name}</td>
              <td>{t.triggerType}</td>
              <td>{t.expression}</td>
              <td>{t.status}</td>
              <td>
                <div className="flex items-center gap-2">
                  <Button variant="secondary" size="small" onClick={() => { toggleTrigger(t.triggerId, t.triggerType, t.status) }}>
                    {t.status === 'active' ? <RiShutDownLine className="h-4 w-4" /> : <RiRestartLine className="h-4 w-4" />}
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

export default WorkflowAppLogList
