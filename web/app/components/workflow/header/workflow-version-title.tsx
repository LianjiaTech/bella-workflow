import { memo } from 'react'
import { useTranslation } from 'react-i18next'
import { useIsChatMode } from '../hooks'
import { useStore } from '../store'
import { ClockPlay } from '@/app/components/base/icons/src/vender/line/time'

const WorkflowVersionTitle = () => {
  const { t } = useTranslation()
  const isChatMode = useIsChatMode()
  const historyWorkflowVersion = useStore(s => s.historyWorkflowVersion)

  return (
    <div className='flex items-center h-[18px] text-xs text-gray-500'>
      <ClockPlay className='mr-1 w-3 h-3 text-gray-500' />
      {
        historyWorkflowVersion && (
          <div>
            <span>{isChatMode ? `版本号#${historyWorkflowVersion?.version}` : `版本号#${historyWorkflowVersion?.version}`}</span>
            <span className='mx-1'>·</span>
          </div>
        )
      }
      <span
        className='ml-1 uppercase flex items-center px-1 h-[18px] rounded-[5px] border border-indigo-300 bg-white/[0.48] text-[10px] font-semibold text-indigo-600'>
        {t('workflow.common.viewOnly')}
      </span>
    </div>
  )
}

export default memo(WorkflowVersionTitle)
