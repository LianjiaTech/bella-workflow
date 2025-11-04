import type { FC } from 'react'
import {
  memo,
  useCallback,
  useMemo,
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
  InputVarType,
} from '../types'
import type { StartNodeType } from '../nodes/start/types'
import {
  useChecklistBeforePublish, useNodesInteractions,
  useNodesReadOnly,
  useNodesSyncDraft, useWorkflowInteractions,
  useWorkflowMode, useWorkflowReadOnly,
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
import CreateFromDSLModal from '@/app/components/app/create-from-dsl-modal'
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
      const res = await publishWorkflowWithReleaseDescription(`/apps/${appID}/workflows/publish`, releaseDescription)
      if (res.code === 200) {
        notify({ type: 'success', message: t('common.api.actionSuccess') })
        workflowStore.getState().setPublishedAt(res.created_at)
      }
      else {
        notify({ type: 'error', message: t(res.message) })
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

  const onExportDSL = useCallback(async (includeSecrets = false) => {
    try {
      const { data } = await exportAppConfig({
        appID: appID!,
        include: includeSecrets,
      })
      const a = document.createElement('a')
      const file = new Blob([data], { type: 'application/yaml' })
      a.href = URL.createObjectURL(file)
      a.download = `${appDetail?.name || 'workflow'}.yml`
      a.click()
    }
    catch (e) {
      notify({ type: 'error', message: t('app.exportFailed') })
    }
  }, [appID, appDetail?.name, notify, t])

  const handleExportDSL = useCallback(async () => {
    if (!appDetail?.mode || (appDetail.mode !== 'workflow' && appDetail.mode !== 'advanced-chat')) {
      // 直接导出
      await onExportDSL()
      return
    }
    try {
      const workflowDraft = await fetchWorkflowDraft(`/apps/${appID}/workflows/draft`)
      const list = (workflowDraft.environment_variables || []).filter(env => env.value_type === 'secret')
      if (list.length === 0) {
        await onExportDSL()
        return
      }
      setSecretEnvList(list)
      setShowExportDSLModal(true)
    }
    catch (e) {
      notify({ type: 'error', message: t('app.exportFailed') })
    }
  }, [appDetail?.mode, appID, notify, t, onExportDSL])

  const onVersionHistory = useCallback(() => {
    handleBackupDraft()
    handleNodesCancelSelected()
    handleCancelDebugAndPreviewPanel()
    workflowStore.setState({ isVersionHistory: true })
  }, [workflowStore, handleCancelDebugAndPreviewPanel, handleBackupDraft, handleNodesCancelSelected])

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

      if (id === 'env') {
        hasEnv = true
        renderedButtons.push(
          <EnvButton key={id} disabled={getWorkflowReadOnly()} />,
        )
      }

      if (id === 'runAndHistory') {
        if (hasEnv)
          renderedButtons.push(<div key={`divider-${index}`} className='w-[1px] h-3.5 bg-gray-200'></div>)

        renderedButtons.push(
          <RunAndHistory key={id} customLabels={labels} />,
        )
      }

      if (id === 'copilot') {
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

      if (id === 'customImportDsl') {
        renderedButtons.push(
          <Button key={id} className='text-components-button-secondary-text px-2' onClick={handleImportDSL}>
            {t('app.importFromDSL')}
          </Button>,
        )
      }

      if (id === 'customExportDsl') {
        renderedButtons.push(
          <Button key={id} className='text-components-button-secondary-text px-2' onClick={handleExportDSL}>
            {t('app.export')}
          </Button>,
        )
      }

      if (id === 'publish') {
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
          versionHistory && <WorkflowVersionTitle />
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
        versionHistory && (
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
        <CreateFromDSLModal
          show={showImportDSLModal}
          onClose={() => setShowImportDSLModal(false)}
          onSuccess={() => {
            setShowImportDSLModal(false)
            // 刷新页面或应用列表
          }}
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
