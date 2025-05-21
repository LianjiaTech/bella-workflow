'use client'
import React, { useEffect } from 'react'
import type { ReactNode } from 'react'
import { usePathname } from 'next/navigation'
import SwrInitor from '@/app/components/swr-initor'
import { AppContextProvider } from '@/context/app-context'
import GA, { GaType } from '@/app/components/base/ga'
import HeaderWrapper from '@/app/components/header/header-wrapper'
import Header from '@/app/components/header'
import { EventEmitterContextProvider } from '@/context/event-emitter'
import { ProviderContextProvider } from '@/context/provider-context'
import { ModalContextProvider } from '@/context/modal-context'
import { getQueryParams, getTenantId, setTenantId } from '@/utils/getQueryParams'
import type { AppConfig } from '@/app/context/app-registry'
import { AppRegistryProvider, getAppConfig, tryLoadConfigFile } from '@/app/context/app-registry'

// Try to load the configuration file, won't error if the file doesn't exist
tryLoadConfigFile()

// Layout implementation component
const LayoutImplementation = ({ children }: { children: ReactNode }) => {
  const pathname = usePathname()

  // Create default configuration
  const defaultConfig: AppConfig = {
    tenantId: 'test',
    showHeader: true,
    HeaderComponent: Header,
  }

  // Get application configuration (using default config as second parameter)
  const appConfig = getAppConfig(pathname, defaultConfig)

  useEffect(() => {
    // Determine tenant ID: URL parameter has priority, then app config, then stored value or default
    const tenantId = getQueryParams('tenant') || appConfig.tenantId || getTenantId() || 'test'
    setTenantId(tenantId)
  }, [pathname, appConfig.tenantId])

  // Determine which Header component to use
  const HeaderComponent = appConfig.HeaderComponent || Header

  return (
    <>
      <GA gaType={GaType.admin} />
      <SwrInitor>
        <AppContextProvider>
          <EventEmitterContextProvider>
            <ProviderContextProvider>
              <ModalContextProvider>
                {appConfig.showHeader && (
                  <HeaderWrapper>
                    <HeaderComponent />
                  </HeaderWrapper>
                )}
                {children}
              </ModalContextProvider>
            </ProviderContextProvider>
          </EventEmitterContextProvider>
        </AppContextProvider>
      </SwrInitor>
    </>
  )
}

// Common layout component
const Layout = ({ children }: { children: ReactNode }) => {
  return (
    <AppRegistryProvider>
      <LayoutImplementation>
        {children}
      </LayoutImplementation>
    </AppRegistryProvider>
  )
}

export default Layout
