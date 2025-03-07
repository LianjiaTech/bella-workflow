import type { FC } from 'react'
import {
  memo,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'
import { RiApps2AddLine, RiMagicFill, RiMagicLine } from '@remixicon/react'
import { useTranslation } from 'react-i18next'
import { useContext } from 'use-context-selector'
import RunAndHistory from './run-and-history'
import {
  useChecklistBeforePublish,
  useNodesReadOnly,
  useNodesSyncDraft,
  useWorkflowMode,
  useWorkflowRun,
} from '@/app/components/workflow/hooks'
import { ToastContext } from '@/app/components/base/toast'
import type { EnvironmentVariable } from '@/app/components/workflow/types'
import { WorkflowRunningStatus } from '@/app/components/workflow/types'
import { useStore, useWorkflowStore } from '@/app/components/workflow/store'
import EditingTitle from '@/app/components/workflow/header/editing-title'
import RunningTitle from '@/app/components/workflow/header/running-title'
import RestoringTitle from '@/app/components/workflow/header/restoring-title'
import ViewHistory from '@/app/components/workflow/header/view-history'
import Button from '@/app/components/base/button'
import { useStore as useAppStore } from '@/app/components/app/store'
import { fetchWorkflowDraft, publishWorkflowWithReleaseDescription } from '@/service/workflow'
import { ArrowNarrowLeft } from '@/app/components/base/icons/src/vender/line/arrows'
import { exportAppConfig } from '@/service/apps'
import UpdateDSLModal from '@/app/components/workflow/update-dsl-modal'
import DSLExportConfirmModal from '@/app/components/workflow/dsl-export-confirm-modal'

const Header: FC = () => {
  const { t } = useTranslation()
  const workflowStore = useWorkflowStore()

  const [secretEnvList, setSecretEnvList] = useState<EnvironmentVariable[]>([])
  const [showImportDSLModal, setShowImportDSLModal] = useState<boolean>(false)

  const appDetail = useAppStore(s => s.appDetail)
  const { getNodesReadOnly } = useNodesReadOnly()
  const showCopilotPanel = useStore(s => s.showCopilotPanel)
  const workflowRunningData = useStore(s => s.workflowRunningData)
  const isExecutePublish = useRef<boolean>(false)

  const appID = appDetail?.id
  const {
    handleLoadBackupDraft,
  } = useWorkflowRun()
  const { handleCheckBeforePublish } = useChecklistBeforePublish()
  const { handleSyncWorkflowDraft } = useNodesSyncDraft()
  const { notify } = useContext(ToastContext)
  const {
    normal,
    restoring,
    viewHistory,
  } = useWorkflowMode()
  const handleShowFeatures = useCallback(() => {
    const {
      showFeaturesPanel,
      isRestoring,
      setShowFeaturesPanel,
    } = workflowStore.getState()
    if (getNodesReadOnly() && !isRestoring)
      return
    setShowFeaturesPanel(!showFeaturesPanel)
  }, [workflowStore, getNodesReadOnly])

  const handleShowCopilot = useCallback(() => {
    workflowStore.setState({ showCopilotPanel: !showCopilotPanel })
  }, [workflowStore, showCopilotPanel])

  const handleCancelRestore = useCallback(() => {
    handleLoadBackupDraft()
    workflowStore.setState({ isRestoring: false })
  }, [workflowStore, handleLoadBackupDraft])

  const handleRestore = useCallback(() => {
    workflowStore.setState({ isRestoring: false })
    workflowStore.setState({ backupDraft: undefined })
    handleSyncWorkflowDraft(true)
  }, [handleSyncWorkflowDraft, workflowStore])
  const [releaseDescription] = useState('')
  const onPublish = useCallback(async () => {
    if (handleCheckBeforePublish()) {
      const res = await publishWorkflowWithReleaseDescription(`/apps/${appID}/workflows/publish`, releaseDescription)
      if (res?.code === 200) {
        notify({ type: 'success', message: t('common.api.actionSuccess') })
        workflowStore.getState().setPublishedAt(res.created_at)
      }
      else {
        notify({ type: 'error', message: t(res?.message) })
      }
    }
    else {
      throw new Error('Checklist failed')
    }
  }, [appID, handleCheckBeforePublish, notify, t, workflowStore])

  useEffect(() => {
    if (workflowRunningData?.result.status === WorkflowRunningStatus.Running)
      isExecutePublish.current = true

    if (workflowRunningData?.result.status === WorkflowRunningStatus.Succeeded && isExecutePublish.current) {
      // 发布
      onPublish()
      // 每次运行后只执行1次发布
      isExecutePublish.current = false
    }
  }, [workflowRunningData?.result.status, onPublish])

  const handleGoBackToEdit = useCallback(() => {
    handleLoadBackupDraft()
    workflowStore.setState({ historyWorkflowData: undefined })
  }, [workflowStore, handleLoadBackupDraft])

  const onExport = async (include = false) => {
    if (!appDetail)
      return
    try {
      const { data } = await exportAppConfig({
        appID: appDetail.id,
        include,
      })
      const a = document.createElement('a')
      const file = new Blob([data], { type: 'application/json' })
      a.href = URL.createObjectURL(file)
      a.download = `${appDetail.id}.json`
      a.click()
    }
    catch (e) {
      notify({ type: 'error', message: t('app.exportFailed') })
    }
  }

  const exportCheck = async () => {
    if (!appDetail)
      return
    if (appDetail.mode !== 'workflow' && appDetail.mode !== 'advanced-chat') {
      onExport()
      return
    }
    try {
      const workflowDraft = await fetchWorkflowDraft(`/apps/${appDetail.id}/workflows/draft`)
      const list = (workflowDraft.environment_variables || []).filter(env => env.value_type === 'secret')
      if (list.length === 0) {
        onExport()
        return
      }
      setSecretEnvList(list)
    }
    catch (e) {
      notify({ type: 'error', message: t('app.exportFailed') })
    }
  }

  return (
    <div
      className='absolute top-0 left-0 z-10 flex items-center justify-between w-full px-3 h-14'
      style={{
        background: 'linear-gradient(180deg, #F9FAFB 0%, rgba(249, 250, 251, 0.00) 100%)',
      }}
    >
      <div>
        {
          normal && <EditingTitle />
        }
        {
          viewHistory && <RunningTitle />
        }
        {
          restoring && <RestoringTitle />
        }
      </div>
      {
        normal && (
          <div className='flex items-center gap-2'>
            <div className='w-[1px] h-3.5 bg-gray-200'></div>
            <RunAndHistory />
            <Button className='text-components-button-secondary-text px-2' onClick={handleShowCopilot}>
              {showCopilotPanel && (
                <RiMagicFill className='w-4 h-4 mr-1 text-components-button-secondary-text' />)
              }
              {!showCopilotPanel && (
                <RiMagicLine className='w-4 h-4 mr-1 text-components-button-secondary-text' />)
              }
              {t('workflow.common.copilot')}
            </Button>
            <Button
              variant='secondary'
              className='pl-3 pr-2'
              onClick={exportCheck}
            >
              {t('app.export')}
            </Button>
            <Button
              variant='secondary'
              className='pl-3 pr-2'
              onClick={() => {
                setShowImportDSLModal(true)
              }}>
              {t('workflow.common.importDSL')}
            </Button>
          </div>
        )
      }
      {
        viewHistory && (
          <div className='flex items-center'>
            <ViewHistory withText />
            <div className='mx-2 w-[1px] h-3.5 bg-gray-200'></div>
            <Button
              variant='primary'
              className='mr-2'
              onClick={handleGoBackToEdit}
            >
              <ArrowNarrowLeft className='w-4 h-4 mr-1' />
              {t('workflow.common.goBackToEdit')}
            </Button>
          </div>
        )
      }
      {
        restoring && (
          <div className='flex items-center'>
            <Button className='text-components-button-secondary-text' onClick={handleShowFeatures}>
              <RiApps2AddLine className='w-4 h-4 mr-1 text-components-button-secondary-text' />
              {t('workflow.common.features')}
            </Button>
            <div className='mx-2 w-[1px] h-3.5 bg-gray-200'></div>
            <Button
              className='mr-2'
              onClick={handleCancelRestore}
            >
              {t('common.operation.cancel')}
            </Button>
            <Button
              onClick={handleRestore}
              variant='primary'
            >
              {t('workflow.common.restore')}
            </Button>
          </div>
        )
      }
      {showImportDSLModal && (
        <UpdateDSLModal
          onCancel={() => setShowImportDSLModal(false)}
          onBackup={() => onExport()}
        />
      )}
      {secretEnvList.length > 0 && (
        <DSLExportConfirmModal
          envList={secretEnvList}
          onConfirm={onExport}
          onClose={() => setSecretEnvList([])}
        />
      )}

    </div>
  )
}

export default memo(Header)
