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
import { getQueryParams, setTenantId } from '@/utils/getQueryParams'
import { TenantConfigCenter } from '@/config/tenant'

const LayoutContent = ({ children }: { children: ReactNode }) => {
  const pathname = usePathname()
  const router = useRouter()
  const { tenantConfigFromStore, setTenantConfigFromStore } = useAppContext()

  const firstSeg = pathname.split('/')[1]
  const secondSeg = pathname.split('/')[2]
  const isAppSection = () => firstSeg === 'app' || firstSeg === 'apps'
  const isGlobalSection = () => firstSeg === 'app' || firstSeg === 'apps' || secondSeg === 'datasources'

  useEffect(() => {
    let finalTenantId: string

    if (isAppSection()) {
      finalTenantId = 'test'
    }
    else {
      const tenantMatch = pathname.match(/^\/([^\/]+)/)
      const tenantIdFromUrl = tenantMatch ? tenantMatch[1] : null

      finalTenantId = tenantIdFromUrl || getQueryParams('tenantId') || 'test'
      const tenantDefaultConfig = TenantConfigCenter.getConfig(finalTenantId)
      setTenantConfigFromStore(tenantDefaultConfig)
    }
    setTenantId(finalTenantId)
  }, [isAppSection, pathname, router, setTenantConfigFromStore])

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
  }, [pathname, tenantConfigFromStore, router, isGlobalSection, secondSeg])

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

const Layout = ({ children }: { children: ReactNode }) => {
  return (
    <>
      <GA gaType={GaType.admin} />
      <SwrInitor>
        <AppContextProvider>
          <EventEmitterContextProvider>
            <ProviderContextProvider>
              <ModalContextProvider>
                <LayoutContent>
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

export default Layout
