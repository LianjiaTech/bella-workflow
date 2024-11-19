'use client'
import type { FC } from 'react'
import { useUnmount } from 'ahooks'
import React, { useCallback, useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import cn from 'classnames'
import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { useContext, useContextSelector } from 'use-context-selector'
import {
  RiTerminalBoxFill,
  RiTerminalBoxLine, RiTerminalFill,
  RiTerminalLine,
  RiTimerFlashFill,
  RiTimerFlashLine,
} from '@remixicon/react'
import s from './style.module.css'
import { useStore } from '@/app/components/app/store'
import type { NavIcon } from '@/app/components/app-sidebar/navLink'
import { fetchAppDetail, updateAppInfo } from '@/service/apps'
import AppsContext, { useAppContext } from '@/context/app-context'
import Loading from '@/app/components/base/loading'
import {
  PromptEngineering,
} from '@/app/components/base/icons/src/vender/line/development'
import {
  PromptEngineering as PromptEngineeringSolid,
} from '@/app/components/base/icons/src/vender/solid/development'
import useBreakpoints, { MediaType } from '@/hooks/use-breakpoints'
import { ToastContext } from '@/app/components/base/toast'
import type { CreateAppModalProps } from '@/app/components/explore/create-app-modal'

export type IAppDetailLayoutProps = {
  children: React.ReactNode
  params: { appId: string }
}

const AppDetailLayout: FC<IAppDetailLayoutProps> = (props) => {
  const {
    children,
    params: { appId }, // get appId in path
  } = props
  const { t } = useTranslation()
  const router = useRouter()
  const pathname = usePathname()
  const media = useBreakpoints()
  const isMobile = media === MediaType.mobile
  const { isCurrentWorkspaceManager } = useAppContext()
  const { appDetail, setAppDetail, setAppSiderbarExpand } = useStore(useShallow(state => ({
    appDetail: state.appDetail,
    setAppDetail: state.setAppDetail,
    setAppSiderbarExpand: state.setAppSiderbarExpand,
  })))

  const { notify } = useContext(ToastContext)
  const [showEditModal, setShowEditModal] = useState(true)

  const [navigation, setNavigation] = useState<Array<{
    name: string
    href: string
    icon: NavIcon
    selectedIcon: NavIcon
  }>>([])

  const mutateApps = useContextSelector(AppsContext, state => state.mutateApps)

  const getNavigations = useCallback((appId: string, isCurrentWorkspaceManager: boolean, mode: string, workflowName: string) => {
    const urlParams = window.location.search
    const navs = [
      ...(isCurrentWorkspaceManager
        ? [{
          name: t('common.appMenus.promptEng'),
          href: `/huiting/${appId}/${(mode === 'workflow' || mode === 'advanced-chat') ? 'workflow' : 'configuration'}?workflowName=${workflowName}`,
          icon: PromptEngineering,
          selectedIcon: PromptEngineeringSolid,
        }]
        : []
      ),
      {
        name: t('common.appMenus.apiAccess'),
        href: `/huiting/${appId}/develop`,
        icon: RiTerminalBoxLine,
        selectedIcon: RiTerminalBoxFill,
      },
      ...(mode === 'workflow'
        ? [{
          name: t('common.appMenus.trigger'),
          href: `/huiting/${appId}/trigger`,
          icon: RiTimerFlashLine,
          selectedIcon: RiTimerFlashFill,
        }]
        : []
      ),
      {
        name: t('common.appMenus.customApi'),
        href: `/huiting/${appId}/customApi`,
        icon: RiTerminalLine,
        selectedIcon: RiTerminalFill,
      },
    ]
    return navs
  }, [t])

  const onEdit: CreateAppModalProps['onConfirm'] = useCallback(async ({
    name,
    icon,
    icon_background,
    description,
  }) => {
    if (!appDetail)
      return
    if (description === '' || description.trim() === '') {
      notify({ type: 'error', message: '描述不能为空' })
      return
    }
    try {
      const app = await updateAppInfo({
        ...appDetail,
        appID: appDetail.id,
        name,
        icon,
        icon_background,
        description,
      })
      setShowEditModal(false)
      notify({
        type: 'success',
        message: t('app.editDone'),
      })
      setAppDetail(app)
      mutateApps()
    }
    catch (e) {
      notify({ type: 'error', message: t('app.editFailed') })
    }
  }, [appDetail, mutateApps, notify, setAppDetail, t])

  useEffect(() => {
    if (appDetail) {
      document.title = `${(appDetail.name || 'App')} - huiting`
      const localeMode = localStorage.getItem('app-detail-collapse-or-expand') || 'expand'
      const mode = isMobile ? 'collapse' : 'expand'
      setAppSiderbarExpand(isMobile ? mode : localeMode)
      // TODO: consider screen size and mode
      // if ((appDetail.mode === 'advanced-chat' || appDetail.mode === 'workflow') && (pathname).endsWith('workflow'))
      //   setAppSiderbarExpand('collapse')
      appDetail.description && setShowEditModal(false)
    }
  }, [appDetail, isMobile])

  useEffect(() => {
    setAppDetail()

    fetchAppDetail({ url: '/apps', id: appId }).then((res) => {
      // redirections
      if ((res.mode === 'workflow' || res.mode === 'advanced-chat') && (pathname).endsWith('configuration')) {
        router.replace(`/huiting/${appId}/workflow?workflowName=${res?.name}`)
      }
      else if ((res.mode !== 'workflow' && res.mode !== 'advanced-chat') && (pathname).endsWith('workflow')) {
        router.replace(`/huiting/${appId}/configuration`)
      }
      else {
        setAppDetail(res)
        setNavigation(getNavigations(appId, isCurrentWorkspaceManager, res.mode, res?.name))
      }
    })
  }, [appId, isCurrentWorkspaceManager])

  useUnmount(() => {
    setAppDetail()
  })

  if (!appDetail) {
    return (
      <div className='flex h-full items-center justify-center bg-white'>
        <Loading />
      </div>
    )
  }

  return (
    <div className={cn(s.app, 'flex', 'overflow-hidden')}>
      {/* <CreateAppModal
        isEditModal
        appIcon={appDetail.icon}
        appIconBackground={appDetail.icon_background}
        appName={appDetail.name}
        appDescription={appDetail.description}
        show={showEditModal}
        onConfirm={onEdit}
        onHide={() => {}}
      /> */}
      <div className="bg-white grow overflow-hidden">
        {children}
      </div>
    </div>
  )
}
export default React.memo(AppDetailLayout)
