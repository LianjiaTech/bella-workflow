'use client'
import React, { useEffect } from 'react'
import type { ReactNode } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import SwrInitor from '@/app/components/swr-initor'
import { AppContextProvider, useAppContext } from '@/context/app-context'
import GA, { GaType } from '@/app/components/base/ga'
import HeaderWrapper from '@/app/components/header/header-wrapper'
import Header from '@/app/components/header'
import { EventEmitterContextProvider } from '@/context/event-emitter'
import { ProviderContextProvider } from '@/context/provider-context'
import { ModalContextProvider } from '@/context/modal-context'
import { setTenantId } from '@/utils/getQueryParams'
import { TenantConfigCenter } from '@/config/tenant'
import { TenantSessionManager } from '@/utils/tenant-session'

type TenantLayoutProps = {
  children: ReactNode
  params: { tenantId: string }
}

const LayoutContent = ({ children, tenantId }: { children: ReactNode; tenantId: string }) => {
  const pathname = usePathname()
  const router = useRouter()
  const { tenantConfigFromStore, setTenantConfigFromStore } = useAppContext()

  // 租户路由下的页面类型检查
  const isAppsSection = () => pathname.includes('/apps')
  const isAppSection = () => pathname.includes('/app/')
  const isDatasetsSection = () => pathname.includes('/datasets')
  const isGlobalSection = () => isAppsSection() || isAppSection() || isDatasetsSection()

  useEffect(() => {
    // 使用URL中的tenantId
    const finalTenantId = tenantId || TenantSessionManager.getDefaultTenant()

    // 更新sessionStorage
    TenantSessionManager.setCurrentTenant(finalTenantId)

    // 获取租户配置
    const tenantDefaultConfig = TenantConfigCenter.getConfig(finalTenantId)
    if (tenantDefaultConfig)
      setTenantConfigFromStore(tenantDefaultConfig)

    // 设置全局tenantId
    setTenantId(finalTenantId)
  }, [tenantId, setTenantConfigFromStore])

  // 根据配置确定是否展示模块
  useEffect(() => {
    const features = tenantConfigFromStore?.appConfig?.features
    if (!features || isGlobalSection())
      return

    const match = pathname.match(/\/(customApi|logs|develop|trigger)(\/|$)/)
    const seg = match?.[1] as 'customApi' | 'logs' | 'develop' | 'trigger' | undefined
    if (!seg)
      return

    const enabled = seg === 'customApi'
      ? !!features.customApi
      : seg === 'logs'
        ? !!features.logs
        : seg === 'develop'
          ? !!features.develop
          : seg === 'trigger'
            ? !!features.trigger
            : true

    if (!enabled)
      router.replace('/404')
  }, [pathname, tenantConfigFromStore, router, isGlobalSection])

  const receiveMessage = (event: MessageEvent) => {
    const { payload } = event.data || {}
    if (payload?.tenantConfig && payload.tenantConfig?.tenantId)
      setTenantConfigFromStore(payload?.tenantConfig)
  }

  useEffect(() => {
    if (isGlobalSection())
      return

    window.addEventListener('message', receiveMessage)

    return () => {
      window.removeEventListener('message', receiveMessage)
    }
  }, [isGlobalSection])

  return (
    <>
      {(tenantConfigFromStore?.appConfig?.appHeader || isGlobalSection())
        && (
          <HeaderWrapper>
            <Header />
          </HeaderWrapper>
        )}
      {children}
    </>
  )
}

const TenantLayout = ({ children, params: { tenantId } }: TenantLayoutProps) => {
  return (
    <>
      <GA gaType={GaType.admin} />
      <SwrInitor>
        <AppContextProvider>
          <EventEmitterContextProvider>
            <ProviderContextProvider>
              <ModalContextProvider>
                <LayoutContent tenantId={tenantId}>
                  {children}
                </LayoutContent>
              </ModalContextProvider>
            </ProviderContextProvider>
          </EventEmitterContextProvider>
        </AppContextProvider>
      </SwrInitor>
    </>
  )
}

export default TenantLayout
