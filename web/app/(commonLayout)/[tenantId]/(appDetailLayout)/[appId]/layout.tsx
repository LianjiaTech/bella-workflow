'use client'
import type { FC } from 'react'
import { useUnmount } from 'ahooks'
import React, { useCallback, useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import cn from 'classnames'
import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { useContext, useContextSelector } from 'use-context-selector'
import { RiFileList3Fill, RiFileList3Line, RiTerminalBoxFill, RiTerminalBoxLine, RiTerminalFill, RiTerminalLine, RiTimerFlashFill, RiTimerFlashLine } from '@remixicon/react'
import s from './style.module.css'
import AppSideBar from '@/app/components/app-sidebar/index'
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
import CreateAppModal from '@/app/components/explore/create-app-modal'
import { ToastContext } from '@/app/components/base/toast'
import type { CreateAppModalProps } from '@/app/components/explore/create-app-modal'

export type IAppDetailLayoutProps = {
  children: React.ReactNode
  params: { appId: string; tenantId: string }
}

const AppDetailLayout: FC<IAppDetailLayoutProps> = (props) => {
  const {
    children,
    params: { appId, tenantId }, // get appId and tenantId in path
  } = props
  const { t } = useTranslation()
  const router = useRouter()
  const pathname = usePathname()
  const media = useBreakpoints()
  const isMobile = media === MediaType.mobile
  const { isCurrentWorkspaceManager, tenantConfigFromStore } = useAppContext()
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
    const navs: Array<{
      name: string
      href: string
      icon: NavIcon
      selectedIcon: NavIcon
    }> = [
      ...(isCurrentWorkspaceManager
        ? [{
          name: t('common.appMenus.promptEng'),
          href: `/${tenantId}/${appId}/${(mode === 'workflow' || mode === 'advanced-chat') ? 'workflow' : 'configuration'}?workflowName=${workflowName}`,
          icon: PromptEngineering as NavIcon,
          selectedIcon: PromptEngineeringSolid as NavIcon,
        }]
        : []
      ),
    ]
    const features = tenantConfigFromStore?.appConfig?.features

    if (features?.develop) {
      navs.push({
        name: t('common.appMenus.apiAccess'),
        href: `/${tenantId}/${appId}/develop`,
        icon: RiTerminalBoxLine as NavIcon,
        selectedIcon: RiTerminalBoxFill as NavIcon,
      })
    }

    if (features?.logs) {
      navs.push({
        name: t('common.appMenus.logs'),
        href: `/${tenantId}/${appId}/logs`,
        icon: RiFileList3Line as NavIcon,
        selectedIcon: RiFileList3Fill as NavIcon,
      })
    }

    if (features?.customApi) {
      navs.push({
        name: t('common.appMenus.customApi'),
        href: `/${tenantId}/${appId}/customApi`,
        icon: RiTerminalLine as NavIcon,
        selectedIcon: RiTerminalFill as NavIcon,
      })
    }

    if (mode === 'workflow' && features?.trigger) {
      navs.push({
        name: t('common.appMenus.trigger'),
        href: `/${tenantId}/${appId}/trigger`,
        icon: RiTimerFlashLine as NavIcon,
        selectedIcon: RiTimerFlashFill as NavIcon,
      })
    }

    return navs
  }, [t, tenantId, tenantConfigFromStore])

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
      document.title = `${(appDetail.name || tenantConfigFromStore?.displayName || 'App')} - ${tenantConfigFromStore?.brand || 'Workflow'}`
      const localeMode = localStorage.getItem('app-detail-collapse-or-expand') || 'expand'
      const mode = isMobile ? 'collapse' : 'expand'
      setAppSiderbarExpand(isMobile ? mode : localeMode)
      appDetail.description && setShowEditModal(false)
    }
  }, [appDetail, isMobile, tenantConfigFromStore])

  useEffect(() => {
    setAppDetail()

    fetchAppDetail({ url: '/apps', id: appId }).then((res) => {
      // redirections
      if ((res.mode === 'workflow' || res.mode === 'advanced-chat') && (pathname).endsWith('configuration'))
        router.replace(`/${tenantId}/${appId}/workflow?workflowName=${res?.name}`)
      else if ((res.mode !== 'workflow' && res.mode !== 'advanced-chat') && (pathname).endsWith('workflow'))
        router.replace(`/${tenantId}/${appId}/configuration`)
      else
        setAppDetail(res)
    })
  }, [appId, isCurrentWorkspaceManager, tenantId])

  // 根据最新 tenantConfigFromStore 和 appDetail 变化，独立更新导航，避免重定向循环
  useEffect(() => {
    if (!appDetail)
      return
    setNavigation(getNavigations(appId, isCurrentWorkspaceManager, appDetail.mode, appDetail?.name))
    // 依赖 getNavigations（已随 tenantId/tenantConfigFromStore 变化而变化）
  }, [appDetail, appId, isCurrentWorkspaceManager, getNavigations])

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
      {tenantConfigFromStore?.appConfig?.features?.workflow?.initialization?.showTitleDescModal && <CreateAppModal
        isEditModal
        appIcon={appDetail.icon}
        appIconBackground={appDetail.icon_background}
        appName={appDetail.name}
        appDescription={appDetail.description}
        show={showEditModal}
        onConfirm={onEdit}
        onHide={() => { }}
      />}
      {(appDetail && tenantConfigFromStore?.appConfig?.appSidebar) && (
        <AppSideBar title={appDetail.name} icon={appDetail.icon} icon_background={appDetail.icon_background} desc={appDetail.mode} navigation={navigation} />
      )}
      <div className="bg-white grow overflow-hidden">
        {children}
      </div>
    </div>
  )
}
export default React.memo(AppDetailLayout)
