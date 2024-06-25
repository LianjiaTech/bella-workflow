'use client'

import { forwardRef, useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useRouter, useSearchParams } from 'next/navigation'
import { useContext, useContextSelector } from 'use-context-selector'
import CreateAppTemplateDialog from '@/app/components/app/create-app-dialog'
import CreateAppModal from '@/app/components/app/create-app-modal'
import CreateFromDSLModal from '@/app/components/app/create-from-dsl-modal'
import { useProviderContext } from '@/context/provider-context'
import { FilePlus01 } from '@/app/components/base/icons/src/vender/line/files'
import { createApp } from '@/service/apps'
import { getRedirection } from '@/utils/app-redirection'
import { NEED_REFRESH_APP_LIST_KEY } from '@/config'
import { ToastContext } from '@/app/components/base/toast'
import AppsContext, { useAppContext } from '@/context/app-context'
export type CreateAppCardProps = {
  onSuccess?: () => void
}

// eslint-disable-next-line react/display-name
const CreateAppCard = forwardRef<HTMLAnchorElement, CreateAppCardProps>(
  ({ onSuccess }, ref) => {
    const { t } = useTranslation()
    const { notify } = useContext(ToastContext)
    const { onPlanInfoChanged } = useProviderContext()
    const { push } = useRouter()
    const searchParams = useSearchParams()
    const [showNewAppTemplateDialog, setShowNewAppTemplateDialog]
      = useState(false)
    const [showNewAppModal, setShowNewAppModal] = useState(false)
    const [showCreateFromDSLModal, setShowCreateFromDSLModal] = useState(false)
    const { isCurrentWorkspaceManager } = useAppContext()
    const mutateApps = useContextSelector(
      AppsContext,
      state => state.mutateApps,
    )
    const [userName, setUserName] = useState(searchParams.get('userName'))
    const [ucid, setUcid] = useState(searchParams.get('ucid'))

    const createAppAndHandleResult = useCallback(async () => {
      const timestamp = new Date().getTime()
      try {
        if (userName && ucid) {
          localStorage.setItem('userName', userName || '')
          localStorage.setItem('ucid', ucid || '')
          const app = await createApp({
            name: searchParams.get('name') || `未命名工作流_${timestamp}`,
            mode: 'workflow',
            description: '',
            icon: '🤖',
            icon_background: '#FFEAD5',
          })
          app.userName = userName
          app.ucid = ucid
          notify({ type: 'success', message: t('app.newApp.appCreated') })
          onPlanInfoChanged()
          if (onSuccess)
            onSuccess()
          mutateApps()
          localStorage.setItem(NEED_REFRESH_APP_LIST_KEY, '1')
          getRedirection(isCurrentWorkspaceManager, app, push)
        }
      }
      catch (e) {
        console.log(e, '创建失败')
      }
    }, [userName, ucid])

    useEffect(() => {
      createAppAndHandleResult()
    }, [])

    return (
      <a
        ref={ref}
        className="relative col-span-1 flex flex-col justify-between min-h-[160px] bg-gray-200 rounded-xl border-[0.5px] border-black/5"
      >
        <div className="grow p-2 rounded-t-xl">
          <div className="px-6 pt-2 pb-1 text-xs font-medium leading-[18px] text-gray-500">
            {t('app.createApp')}
          </div>
          <div
            className="flex items-center mb-1 px-6 py-[7px] rounded-lg text-[13px] font-medium leading-[18px] text-gray-600 cursor-pointer hover:text-primary-600 hover:bg-white"
            onClick={() => setShowNewAppModal(true)}
          >
            <FilePlus01 className="shrink-0 mr-2 w-4 h-4" />
            {t('app.newApp.startFromBlank')}
          </div>
          {/* <div className='flex items-center px-6 py-[7px] rounded-lg text-[13px] font-medium leading-[18px] text-gray-600 cursor-pointer hover:text-primary-600 hover:bg-white' onClick={() => setShowNewAppTemplateDialog(true)}>
          <FilePlus02 className='shrink-0 mr-2 w-4 h-4' />
          {t('app.newApp.startFromTemplate')}
        </div>
      </div>
      <div
        className='p-2 border-t-[0.5px] border-black/5 rounded-b-xl'
        onClick={() => setShowCreateFromDSLModal(true)}
      >
        <div className='flex items-center px-6 py-[7px] rounded-lg text-[13px] font-medium leading-[18px] text-gray-600 cursor-pointer hover:text-primary-600 hover:bg-white'>
          <FileArrow01 className='shrink-0 mr-2 w-4 h-4' />
          {t('app.importDSL')}
        </div> */}
        </div>
        <CreateAppModal
          show={showNewAppModal}
          onClose={() => setShowNewAppModal(false)}
          onSuccess={() => {
            onPlanInfoChanged()
            if (onSuccess)
              onSuccess()
          }}
        />
        <CreateAppTemplateDialog
          show={showNewAppTemplateDialog}
          onClose={() => setShowNewAppTemplateDialog(false)}
          onSuccess={() => {
            onPlanInfoChanged()
            if (onSuccess)
              onSuccess()
          }}
        />
        <CreateFromDSLModal
          show={showCreateFromDSLModal}
          onClose={() => setShowCreateFromDSLModal(false)}
          onSuccess={() => {
            onPlanInfoChanged()
            if (onSuccess)
              onSuccess()
          }}
        />
      </a>
    )
  },
)

export default CreateAppCard
