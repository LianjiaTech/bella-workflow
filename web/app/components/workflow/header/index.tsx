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
import { useNodes } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { useContext } from 'use-context-selector'
import {
  useStore,
  useWorkflowStore,
} from '../store'
import {
  BlockEnum,
  HeaderButtonType,
  InputVarType,
  WorkflowRunningStatus,
} from '../types'
import type { StartNodeType } from '../nodes/start/types'
import {
  useChecklistBeforePublish,
  useNodesInteractions,
  useNodesReadOnly,
  useNodesSyncDraft,
  useWorkflowInteractions,
  useWorkflowMode,
  useWorkflowReadOnly,
  useWorkflowRun,
} from '../hooks'
import AppPublisher from '../../app/app-publisher'
import { ToastContext } from '../../base/toast'
import RunAndHistory from './run-and-history'
import EditingTitle from './editing-title'
import RunningTitle from './running-title'
import RestoringTitle from './restoring-title'
import ViewHistory from './view-history'
import EnvButton from './env-button'
import Button from '@/app/components/base/button'
import { useStore as useAppStore } from '@/app/components/app/store'
import { fetchWorkflowDraft, publishWorkflowWithReleaseDescription } from '@/service/workflow'
import { ArrowNarrowLeft } from '@/app/components/base/icons/src/vender/line/arrows'
import { useFeatures } from '@/app/components/base/features/hooks'
import ViewWorkflowVersionHistory from '@/app/components/workflow/header/view-workflow-version-history'
import WorkflowVersionTitle from '@/app/components/workflow/header/workflow-version-title'
import { useAppContext } from '@/context/app-context'
import UpdateDSLModal from '@/app/components/workflow/update-dsl-modal'
import { exportAppConfig } from '@/service/apps'
import DSLExportConfirmModal from '@/app/components/workflow/dsl-export-confirm-modal'
import type { EnvironmentVariable } from '@/app/components/workflow/types'

const Header: FC = () => {
  const { t } = useTranslation()
  const workflowStore = useWorkflowStore()
  const showCopilotPanel = useStore(s => s.showCopilotPanel)
  const appDetail = useAppStore(s => s.appDetail)
  const appSidebarExpand = useAppStore(s => s.appSidebarExpand)
  const { tenantConfigFromStore } = useAppContext()
  const appID = appDetail?.id
  const { getNodesReadOnly } = useNodesReadOnly()
  const publishedAt = useStore(s => s.publishedAt)
  const draftUpdatedAt = useStore(s => s.draftUpdatedAt)
  const toolPublished = useStore(s => s.toolPublished)
  const nodes = useNodes<StartNodeType>()
  const startNode = nodes.find(node => node.data.type === BlockEnum.Start)
  const startVariables = startNode?.data.variables
  const fileSettings = useFeatures(s => s.features.file)
  const [releaseDescription, setReleaseDescription] = useState('')
  const [showImportDSLModal, setShowImportDSLModal] = useState(false)
  const [showExportDSLModal, setShowExportDSLModal] = useState(false)
  const [secretEnvList, setSecretEnvList] = useState<EnvironmentVariable[]>([])
  const workflowRunningData = useStore(s => s.workflowRunningData)
  const isExecutePublish = useRef<boolean>(false)

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
  }, [appID, handleCheckBeforePublish, notify, releaseDescription, t, workflowStore])

  const onStartRestoring = useCallback(() => {
    workflowStore.setState({ isRestoring: true })
    handleBackupDraft()
    handleRestoreFromPublishedWorkflow()
  }, [handleBackupDraft, handleRestoreFromPublishedWorkflow, workflowStore])

  const onPublisherToggle = useCallback((state: boolean) => {
    if (state)
      handleSyncWorkflowDraft(true)
  }, [handleSyncWorkflowDraft])

  // 自动发布逻辑
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
  }, [workflowRunningData?.result.status, onPublish, tenantConfigFromStore])

  const handleGoBackToEdit = useCallback(() => {
    handleLoadBackupDraft()
    workflowStore.setState({ historyWorkflowData: undefined, historyWorkflowVersion: undefined, isVersionHistory: false })
  }, [workflowStore, handleLoadBackupDraft])

  const handleToolConfigureUpdate = useCallback(() => {
    workflowStore.setState({ toolPublished: true })
  }, [workflowStore])

  const handleDescription = useCallback((desc: string) => {
    setReleaseDescription(desc)
  }, [setReleaseDescription])

  const handleImportDSL = useCallback(() => {
    setShowImportDSLModal(true)
  }, [])

  const onExportDSL = useCallback(async (include = false) => {
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
  }, [appDetail, notify, t])

  const handleExportDSL = useCallback(async () => {
    if (!appDetail)
      return
    if (appDetail.mode !== 'workflow' && appDetail.mode !== 'advanced-chat') {
      onExportDSL()
      return
    }
    try {
      const workflowDraft = await fetchWorkflowDraft(`/apps/${appDetail.id}/workflows/draft`)
      const list = (workflowDraft.environment_variables || []).filter(env => env.value_type === 'secret')
      if (list.length === 0) {
        onExportDSL()
        return
      }
      setSecretEnvList(list)
      setShowExportDSLModal(true)
    }
    catch (e) {
      notify({ type: 'error', message: t('app.exportFailed') })
    }
  }, [appDetail, notify, t, onExportDSL])

  const onVersionHistory = useCallback(() => {
    handleBackupDraft()
    handleNodesCancelSelected()
    handleCancelDebugAndPreviewPanel()
    workflowStore.setState({ isVersionHistory: true })
  }, [workflowStore, handleCancelDebugAndPreviewPanel, handleBackupDraft, handleNodesCancelSelected])

  // 获取按钮权限
  const getButtonPermissions = useCallback((targetButton: string) => {
    const buttonsConfig = tenantConfigFromStore?.appConfig?.features?.workflow?.features?.header?.buttons || []
    const index = buttonsConfig.findIndex(button => button.id === targetButton)
    return index !== -1
  }, [tenantConfigFromStore])

  // 获取按钮标签
  const getButtonLabel = useCallback((targetButton: string, type: 'label' | 'labels' = 'label') => {
    const buttonsConfig = tenantConfigFromStore?.appConfig?.features?.workflow?.features?.header?.buttons || []
    const index = buttonsConfig.findIndex(button => button.id === targetButton)
    if (index !== -1) {
      if (type === 'label')
        return buttonsConfig[index]?.label || ''
      else
        return buttonsConfig[index]?.labels || {}
    }
    return type === 'label' ? '' : {}
  }, [tenantConfigFromStore])

  // 渲染租户配置的按钮
  const renderTenantButtons = useCallback(() => {
    const buttons = tenantConfigFromStore?.appConfig?.features?.workflow?.features?.header?.buttons || []

    // 如果没有配置按钮，则使用默认按钮
    if (buttons.length === 0) {
      return (
        <>
          <EnvButton disabled={getWorkflowReadOnly()} />
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
          <AppPublisher
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
            }}
          />
        </>
      )
    }

    // 基于租户配置渲染按钮
    const renderedButtons = []
    let hasEnv = false

    buttons.forEach((button, index) => {
      const { id, labels, label } = button

      if (id === HeaderButtonType.env) {
        hasEnv = true
        renderedButtons.push(
          <EnvButton key={id} disabled={getWorkflowReadOnly()} />,
        )
      }

      if (id === HeaderButtonType.runAndHistory) {
        if (hasEnv)
          renderedButtons.push(<div key={`divider-${index}`} className='w-[1px] h-3.5 bg-gray-200'></div>)

        const showStopButton = button.showStopButton === undefined ? true : button.showStopButton

        renderedButtons.push(
          <RunAndHistory key={id} labels={labels} showStopButton={showStopButton} />,
        )
      }

      if (id === HeaderButtonType.copilot) {
        renderedButtons.push(
          <Button key={id} className='text-components-button-secondary-text px-2' onClick={handleShowCopilot}>
            {showCopilotPanel && (
              <RiMagicFill className='w-4 h-4 mr-1 text-components-button-secondary-text' />)
            }
            {!showCopilotPanel && (
              <RiMagicLine className='w-4 h-4 mr-1 text-components-button-secondary-text' />)
            }
            {label || t('workflow.common.copilot')}
          </Button>,
        )
      }

      if (id === HeaderButtonType.customImportDsl) {
        renderedButtons.push(
          <Button key={id} variant='secondary' className='pl-3 pr-2' onClick={handleImportDSL}>
            {label || t('workflow.common.importDSL')}
          </Button>,
        )
      }

      if (id === HeaderButtonType.customExportDsl) {
        renderedButtons.push(
          <Button key={id} variant='secondary' className='pl-3 pr-2' onClick={handleExportDSL}>
            {label || t('app.export')}
          </Button>,
        )
      }

      if (id === HeaderButtonType.publish) {
        renderedButtons.push(
          <AppPublisher
            key={id}
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
              label: label as string,
            }}
          />,
        )
      }
    })

    return renderedButtons
  }, [
    tenantConfigFromStore,
    getWorkflowReadOnly,
    showCopilotPanel,
    handleShowCopilot,
    handleImportDSL,
    handleExportDSL,
    publishedAt,
    draftUpdatedAt,
    getNodesReadOnly,
    toolPublished,
    variables,
    handleToolConfigureUpdate,
    onPublish,
    onStartRestoring,
    onPublisherToggle,
    onVersionHistory,
    releaseDescription,
    handleDescription,
    t,
  ])
  return (
    <div
      className='absolute top-0 left-0 z-10 flex items-center justify-between w-full px-3 h-14'
      style={{
        background: 'linear-gradient(180deg, #F9FAFB 0%, rgba(249, 250, 251, 0.00) 100%)',
      }}
    >
      <div>
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
        {
          versionHistory && getButtonPermissions(HeaderButtonType.publish) && <WorkflowVersionTitle />
        }
      </div>
      {
        normal && (
          <div className='flex items-center gap-2'>
            {renderTenantButtons()}
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
          onBackup={() => onExportDSL()}
        />
      )}
      {showExportDSLModal && secretEnvList.length > 0 && (
        <DSLExportConfirmModal
          envList={secretEnvList}
          onConfirm={onExportDSL}
          onClose={() => {
            setShowExportDSLModal(false)
            setSecretEnvList([])
          }}
        />
      )}
    </div>
  )
}

export default memo(Header)
