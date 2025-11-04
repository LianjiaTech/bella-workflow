import type { FC } from 'react'
import {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { RiApps2AddLine, RiMagicFill, RiMagicLine } from '@remixicon/react'
import { useTranslation } from 'react-i18next'
import { useContext } from 'use-context-selector'
import { useNodes } from 'reactflow'
import { HeaderButtonType } from '../types'
import RunAndHistory from './run-and-history'
import {
  useChecklistBeforePublish,
  useNodesInteractions,
  useNodesReadOnly,
  useNodesSyncDraft,
  useWorkflowInteractions,
  useWorkflowMode,
  useWorkflowReadOnly,
  useWorkflowRun,
} from '@/app/components/workflow/hooks'
import { ToastContext } from '@/app/components/base/toast'
import type { EnvironmentVariable } from '@/app/components/workflow/types'
import { BlockEnum, InputVarType, WorkflowRunningStatus } from '@/app/components/workflow/types'
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
import WorkflowVersionTitle from '@/app/components/workflow/header/workflow-version-title'
import EnvButton from '@/app/components/workflow/header/env-button'
import AppPublisher from '@/app/components/app/app-publisher'
import type { StartNodeType } from '@/app/components/workflow/nodes/start/types'
import { useFeatures } from '@/app/components/base/features/hooks'
import ViewWorkflowVersionHistory from '@/app/components/workflow/header/view-workflow-version-history'
import { useAppContext } from '@/context/app-context'

const Header: FC = () => {
  const { t } = useTranslation()
  const workflowStore = useWorkflowStore()

  const [secretEnvList, setSecretEnvList] = useState<EnvironmentVariable[]>([])
  const [showImportDSLModal, setShowImportDSLModal] = useState<boolean>(false)

  const { tenantConfigFromStore } = useAppContext()
  const appDetail = useAppStore(s => s.appDetail)
  const appSidebarExpand = useAppStore(s => s.appSidebarExpand)
  const { getNodesReadOnly } = useNodesReadOnly()
  const publishedAt = useStore(s => s.publishedAt)
  const draftUpdatedAt = useStore(s => s.draftUpdatedAt)
  const toolPublished = useStore(s => s.toolPublished)
  const showCopilotPanel = useStore(s => s.showCopilotPanel)
  const nodes = useNodes<StartNodeType>()
  const startNode = nodes.find(node => node.data.type === BlockEnum.Start)

  const startVariables = startNode?.data.variables
  const fileSettings = useFeatures(s => s.features.file)
  const [releaseDescription, setReleaseDescription] = useState('')

  const workflowRunningData = useStore(s => s.workflowRunningData)
  const isExecutePublish = useRef<boolean>(false)

  const appID = appDetail?.id

  const variables = useMemo(() => {
    const data = startVariables || []
    if (fileSettings?.image?.enabled) {
      return [
        ...data,
        {
          type: InputVarType.files,
          variable: '__image',
          required: false,
          label: 'files',
        },
      ]
    }

    return data
  }, [fileSettings?.image?.enabled, startVariables])

  const {
    handleLoadBackupDraft,
    handleBackupDraft,
    handleRestoreFromPublishedWorkflow,
  } = useWorkflowRun()
  const { handleCheckBeforePublish } = useChecklistBeforePublish()
  const { handleSyncWorkflowDraft } = useNodesSyncDraft()
  const { notify } = useContext(ToastContext)
  const {
    getWorkflowReadOnly,
  } = useWorkflowReadOnly()
  const {
    normal,
    restoring,
    viewHistory,
    versionHistory,
  } = useWorkflowMode()

  const {
    handleNodesCancelSelected,
  } = useNodesInteractions()

  const {
    handleCancelDebugAndPreviewPanel,
  } = useWorkflowInteractions()

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

  const onPublish = useCallback(async () => {
    if (handleCheckBeforePublish()) {
      const res = await publishWorkflowWithReleaseDescription(`/apps/${appID}/workflows/publish`, releaseDescription) as any
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
  }, [appID, handleCheckBeforePublish, notify, t, workflowStore, releaseDescription])

  const onStartRestoring = useCallback(() => {
    workflowStore.setState({ isRestoring: true })
    handleBackupDraft()
    handleRestoreFromPublishedWorkflow()
  }, [handleBackupDraft, handleRestoreFromPublishedWorkflow, workflowStore])

  const onPublisherToggle = useCallback((state: boolean) => {
    if (state)
      handleSyncWorkflowDraft(true)
  }, [handleSyncWorkflowDraft])

  useEffect(() => {
    if (!tenantConfigFromStore?.appConfig?.features?.workflow?.initialization?.autoPublish)
      return

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
    workflowStore.setState({ historyWorkflowData: undefined, historyWorkflowVersion: undefined, isVersionHistory: false })
  }, [workflowStore, handleLoadBackupDraft])

  const handleToolConfigureUpdate = useCallback(() => {
    workflowStore.setState({ toolPublished: true })
  }, [workflowStore])

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

  const handleDescription = useCallback((desc: string) => {
    setReleaseDescription(desc)
  }, [setReleaseDescription])

  const onVersionHistory = useCallback(() => {
    handleBackupDraft()
    handleNodesCancelSelected()
    handleCancelDebugAndPreviewPanel()
    workflowStore.setState({ isVersionHistory: true })
  }, [workflowStore, handleCancelDebugAndPreviewPanel, handleBackupDraft, handleNodesCancelSelected])

  const buttonsConfig = useMemo(() => {
    return tenantConfigFromStore?.appConfig?.features?.workflow?.features?.header?.buttons || []
  }, [tenantConfigFromStore])

  const getButtonPermissions = (targetButton: string) => {
    const index = buttonsConfig.findIndex(button => button.id === targetButton)
    if (index !== -1)
      return true
    else return false
  }

  const getButtonLabel = (targetButton: string, type: 'label' | 'labels' = 'label') => {
    const index = buttonsConfig.findIndex(button => button.id === targetButton)
    if (index !== -1) {
      if (type === 'label')
        return buttonsConfig[index]?.label || ''
      else
        return buttonsConfig[index]?.labels || {}
    }
    else { return '' }
  }

  return (
    <div
      className='absolute top-0 left-0 z-10 flex items-center justify-between w-full px-3 h-14'
      style={{
        background: 'linear-gradient(180deg, #F9FAFB 0%, rgba(249, 250, 251, 0.00) 100%)',
      }}
    >
      <div>
        {/* 贝拉专属 todo */}
        {
          appSidebarExpand === 'collapse' && (
            <div className='text-xs font-medium text-gray-700'>{appDetail?.name}</div>
          )
        }
        {
          normal && <EditingTitle />
        }
        {
          viewHistory && <RunningTitle />
        }
        {
          restoring && <RestoringTitle />
        }
        {/* 贝拉专属  有发布按钮就带着这个按钮 todo */}
        {
          versionHistory
          && getButtonPermissions(HeaderButtonType.publish)
          && <WorkflowVersionTitle />
        }
      </div>
      {
        normal && (
          <div className='flex items-center gap-2'>
            {/* env button todo */}
            {getButtonPermissions(HeaderButtonType.env) && <>
              <EnvButton disabled={getWorkflowReadOnly()} />
              <div className='w-[1px] h-3.5 bg-gray-200'></div>
            </>}
            {getButtonPermissions(HeaderButtonType.runAndHistory) && <RunAndHistory labels={getButtonLabel(HeaderButtonType.runAndHistory, 'labels') as any} />}
            {getButtonPermissions(HeaderButtonType.copilot) && (
              <Button className='text-components-button-secondary-text px-2' onClick={handleShowCopilot}>
                {showCopilotPanel && (
                  <RiMagicFill className='w-4 h-4 mr-1 text-components-button-secondary-text' />)
                }
                {!showCopilotPanel && (
                  <RiMagicLine className='w-4 h-4 mr-1 text-components-button-secondary-text' />)
                }
                {getButtonLabel(HeaderButtonType.copilot) as string || t('workflow.common.copilot')}
              </Button>
            )}
            {getButtonPermissions(HeaderButtonType.publish)
              && <AppPublisher
                {...{
                  publishedAt,
                  draftUpdatedAt,
                  disabled: Boolean(getNodesReadOnly()),
                  toolPublished,
                  inputs: variables,
                  onRefreshData: handleToolConfigureUpdate,
                  onPublish,
                  onRestore: onStartRestoring,
                  onToggle: onPublisherToggle,
                  crossAxisOffset: 4,
                  onVersionHistory,
                  releaseDescription,
                  handleDescription,
                  label: getButtonLabel(HeaderButtonType.publish) as string,
                }}
              />}
            {getButtonPermissions(HeaderButtonType.customExportDsl) && (
              <Button
                variant='secondary'
                className='pl-3 pr-2'
                onClick={exportCheck}
              >
                {getButtonLabel(HeaderButtonType.customExportDsl) as string || t('app.export')}
              </Button>
            )}
            {getButtonPermissions(HeaderButtonType.customImportDsl) && (
              <Button
                variant='secondary'
                className='pl-3 pr-2'
                onClick={() => {
                  setShowImportDSLModal(true)
                }}>
                {getButtonLabel(HeaderButtonType.customImportDsl) as string || t('workflow.common.importDSL')}
              </Button>
            )}
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
      {
        (versionHistory && getButtonPermissions(HeaderButtonType.publish)) && (
          <div className='flex items-center'>
            <ViewWorkflowVersionHistory withText handleGoBackToEdit={handleGoBackToEdit} />
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
