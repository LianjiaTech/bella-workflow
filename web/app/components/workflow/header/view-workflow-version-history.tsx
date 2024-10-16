import {
  memo, useCallback, useEffect,
  useState,
} from 'react'
import { useStoreApi } from 'reactflow'
import useSWR from 'swr'
import { useTranslation } from 'react-i18next'
import { useContext } from 'use-context-selector'
import { useShallow } from 'zustand/react/shallow'
import {
  RiCloseLine,
} from '@remixicon/react'
import {
  useIsChatMode,
  useWorkflow, useWorkflowHistory,
  useWorkflowRun,
} from '../hooks'
import type { HistoryWorkflowVersion } from '../types'
import { ToastContext } from '@/app/components/base/toast'
import cn from '@/utils/classnames'
import {
  PortalToFollowElem,
  PortalToFollowElemContent,
  PortalToFollowElemTrigger,
} from '@/app/components/base/portal-to-follow-elem'
import TooltipPlus from '@/app/components/base/tooltip-plus'
import { useStore as useAppStore } from '@/app/components/app/store'
import {
  ClockPlay,
  ClockPlaySlim,
} from '@/app/components/base/icons/src/vender/line/time'
import {
  activateWorkflowVersion, deactivateWorkflowVersion, fetchDefaultWorkflowVersion,
  fetchWorkflowVersionHistory,
} from '@/service/workflow'
import Loading from '@/app/components/base/loading'
import {
  useStore,
  useWorkflowStore,
} from '@/app/components/workflow/store'

type ViewHistoryProps = {
  withText?: boolean
}
const ViewWorkflowVersionHistory = ({
  withText,
}: ViewHistoryProps) => {
  const { t } = useTranslation()
  const isChatMode = useIsChatMode()
  const { notify } = useContext(ToastContext)
  const [open, setOpen] = useState(true)
  const { formatTimeFromNow } = useWorkflow()
  const workflowStore = useWorkflowStore()
  const { appDetail, setCurrentLogItem, setShowMessageLogModal } = useAppStore(useShallow(state => ({
    appDetail: state.appDetail,
    setCurrentLogItem: state.setCurrentLogItem,
    setShowMessageLogModal: state.setShowMessageLogModal,
  })))
  const historyWorkflowVersion = useStore(s => s.historyWorkflowVersion)
  const { handleBackupDraft } = useWorkflowRun()
  const { data: runList, isLoading: runListLoading } = useSWR((appDetail && open) ? `/apps/${appDetail.id}/workflow-versions` : null, fetchWorkflowVersionHistory)
  const reactflowStore = useStoreApi()
  const isLoading = runListLoading
  const { store, getHistoryLabel } = useWorkflowHistory()
  const [isHoverVersion, setHoverVersion] = useState(0)
  const [defaultVersion, setDefaultVersion] = useState<HistoryWorkflowVersion>()
  const data = runList
  data?.data.forEach((item, index) => {
    item.id = data?.total - index
  })
  useEffect(() => {
    const fetchData = async () => {
      const response = await fetchDefaultWorkflowVersion(`/apps/${appDetail!.id}/workflow-versions/default`)
      setDefaultVersion(response)
      return response
    }
    fetchData()
  }, [setDefaultVersion])
  const handleSetState = useCallback((version: HistoryWorkflowVersion) => {
    const { setEdges, setNodes } = reactflowStore.getState()
    workflowStore.setState({ historyWorkflowVersion: version })
    setEdges(version.graph.edges)
    setNodes(version.graph.nodes)
  }, [reactflowStore, store])

  const handleRestore = useCallback((workflow: HistoryWorkflowVersion) => {
    handleSetState(workflow)
    workflowStore.setState({ backupDraft: undefined })
    handleBackupDraft()
    notify({
      type: 'success',
      message: `已经将版本${workflow.id}还原到草稿`,
    })
  }, [handleSetState, handleBackupDraft])

  const handleSetDefaultVersion = useCallback(async (workflow: HistoryWorkflowVersion) => {
    try {
      const defaultWorkflow = await activateWorkflowVersion({
        workflowId: workflow.workflowId,
        version: workflow.version,
      })
      setDefaultVersion(defaultWorkflow)
      notify({
        type: 'success',
        message: `已经将版本 ${workflow.id} 设置为默认版本`,
      })
    }
    catch (err) {
      notify({
        type: 'error',
        message: `${err}`,
      })
    }
  }, [defaultVersion])

  const handleCancelDefaultVersion = useCallback(async (workflow: HistoryWorkflowVersion) => {
    try {
      const defaultWorkflow = await deactivateWorkflowVersion({
        workflowId: workflow.workflowId,
      })
      setDefaultVersion(defaultWorkflow)
      notify({
        type: 'success',
        message: t('workflow.common.version.cancel'),
      })
    }
    catch (err) {
      notify({
        type: 'error',
        message: `${err}`,
      })
    }
  }, [defaultVersion])
  return (
    (
      <PortalToFollowElem
        placement={withText ? 'bottom-start' : 'bottom-end'}
        offset={{
          mainAxis: 4,
          crossAxis: withText ? -8 : 10,
        }}
        open={open}
        onOpenChange={setOpen}
      >
        <PortalToFollowElemTrigger onClick={() => setOpen(v => !v)}>
          {
            withText && (
              <div className={cn(
                'flex items-center px-3 h-8 rounded-lg border-[0.5px] border-gray-200 bg-white shadow-xs',
                'text-[13px] font-medium text-primary-600 cursor-pointer',
                open && '!bg-primary-50',
              )}>
                <ClockPlay
                  className={'mr-1 w-4 h-4'}
                />
                {t('workflow.common.historyVersion')}
              </div>
            )
          }
          {
            !withText && (
              <TooltipPlus
                popupContent={t('workflow.common.viewRunHistory')}
              >
                <div
                  className={cn('group flex items-center justify-center w-7 h-7 rounded-md hover:bg-state-accent-hover cursor-pointer', open && 'bg-state-accent-hover')}
                  onClick={() => {
                    setCurrentLogItem()
                    setShowMessageLogModal(false)
                  }}
                >
                  <ClockPlay className={cn('w-4 h-4 group-hover:text-components-button-secondary-accent-text', open ? 'text-components-button-secondary-accent-text' : 'text-components-button-ghost-text')} />
                </div>
              </TooltipPlus>
            )
          }
        </PortalToFollowElemTrigger>
        <PortalToFollowElemContent className='z-[12]'>
          <div
            className='flex flex-col ml-2 w-[320px] bg-white border-[0.5px] border-gray-200 shadow-xl rounded-xl overflow-y-auto'
            style={{
              maxHeight: 'calc(2 / 3 * 100vh)',
            }}
          >
            <div className='sticky top-0 bg-white flex items-center justify-between px-4 pt-3 text-base font-semibold text-gray-900'>
              <div className='grow'>{t('workflow.common.historyVersion')}</div>
              <div
                className='shrink-0 flex items-center justify-center w-6 h-6 cursor-pointer'
                onClick={() => {
                  setCurrentLogItem()
                  setShowMessageLogModal(false)
                  setOpen(false)
                }}
              >
                <RiCloseLine className='w-4 h-4 text-gray-500' />
              </div>
            </div>
            {
              isLoading && (
                <div className='flex items-center justify-center h-10'>
                  <Loading />
                </div>
              )
            }
            {
              !isLoading && (
                <div className='p-2'>
                  {
                    !data?.data.length && (
                      <div className='py-12'>
                        <ClockPlaySlim className='mx-auto mb-2 w-8 h-8 text-gray-300' />
                        <div className='text-center text-[13px] text-gray-400'>
                          {t('workflow.common.notRunning')}
                        </div>
                      </div>
                    )
                  }
                  {
                    data?.data.map(item => (
                      <div
                        key={item.id}
                        className={cn(
                          'flex justify-between mb-0.5 px-2 py-[7px] rounded-lg hover:bg-primary-50 cursor-pointer',
                          item.version === historyWorkflowVersion?.version && 'bg-primary-50',
                        )}
                        onClick={() => {
                          workflowStore.setState({
                            showInputsPanel: false,
                            showEnvPanel: false,
                          })
                          handleSetState(item)
                        }}
                        onMouseEnter={() => {
                          setHoverVersion(item.version)
                        }}
                        onMouseLeave={() => {
                          setHoverVersion(-1)
                        }}
                      >
                        <div>
                          <div
                            className={cn(
                              'flex items-center text-[13px] font-medium leading-[18px]',
                              item.version === historyWorkflowVersion?.version && 'text-primary-600',
                            )}
                          >
                            第{item.id}版{item.version === defaultVersion?.defaultPublishVersion ? t('workflow.common.version.current') : ''}
                          </div>
                          <div className="flex items-center text-xs text-gray-500 leading-[18px]" >
                            ·
                          </div>
                          <div className="flex items-center text-xs text-gray-500 leading-[18px]">
                            {item.muName} · {formatTimeFromNow(item.version)}
                          </div>
                        </div>

                        {
                          (
                            <div
                              className={cn(
                                'flex flex-col items-center text-[13px] font-medium leading-[18px]',
                                item.version !== defaultVersion?.defaultPublishVersion ? 'justify-between' : 'justify-end',
                              )}
                            >
                              {
                                isHoverVersion === item.version && (
                                  <div
                                    className="flex items-center text-xs text-gray-500 leading-[18px] px-3 h-6 rounded-lg border-[0.5px] border-gray-200 bg-white shadow-xs text-[13px] font-medium text-primary-600 cursor-pointer !bg-primary-50"
                                    onClick={() => {
                                      handleRestore(item)
                                    }}
                                  >
                                    {t('workflow.common.version.restore')}
                                  </div>
                                )
                              }
                              {
                                isHoverVersion === item.version && item.version !== defaultVersion?.defaultPublishVersion && (
                                  <div
                                    className="flex items-center text-xs text-gray-500 leading-[18px] px-3 h-6 rounded-lg border-[0.5px] border-gray-200 bg-white shadow-xs text-[13px] font-medium text-green-600 cursor-pointer !bg-green-50"
                                    onClick={() => {
                                      handleSetDefaultVersion(item)
                                    }}
                                  >
                                    {t('workflow.common.version.default')}
                                  </div>
                                )
                              }
                              {
                                item.version === defaultVersion?.defaultPublishVersion && (
                                  <div
                                    className="flex items-center text-xs text-gray-500 leading-[18px] px-3 h-6 rounded-lg border-[0.5px] border-gray-200 bg-white shadow-xs text-[13px] font-medium text-red-600 cursor-pointer !bg-red-50"
                                    onClick={() => {
                                      handleCancelDefaultVersion(item)
                                    }}
                                  >
                                    {t('workflow.common.version.cancel')}
                                  </div>
                                )
                              }

                            </div>
                          )
                        }
                      </div>
                    ))
                  }
                </div>
              )
            }
          </div>
        </PortalToFollowElemContent>
      </PortalToFollowElem>
    )
  )
}

export default memo(ViewWorkflowVersionHistory)
