'use client'
import React from 'react'
import type { ReactNode } from 'react'
import { usePathname } from 'next/navigation'
import SwrInitor from '@/app/components/swr-initor'
import { AppContextProvider } from '@/context/app-context'
import GA, { GaType } from '@/app/components/base/ga'
import HeaderWrapper from '@/app/components/header/header-wrapper'
import Header from '@/app/components/header'
import BellaHeader from '@/app/(commonLayout)/bella/components/header'
import { EventEmitterContextProvider } from '@/context/event-emitter'
import { ProviderContextProvider } from '@/context/provider-context'
import { ModalContextProvider } from '@/context/modal-context'
import { setTenantId } from '@/utils/getQueryParams'

const Layout = ({ children }: { children: ReactNode }) => {
  const pathname = usePathname()
  const isBella = pathname.startsWith('/bella')
  const isHuiting = pathname.startsWith('/huiting')
  let tenantId = 'test'
  if (pathname.startsWith('/bella'))
    tenantId = '04633c4f-8638-43a3-a02e-af23c29f821f'
  else if (pathname.startsWith('/huiting'))
    tenantId = 'TENT-d815410c-f9db-459e-b4ab-67a52d8e63ce'
  setTenantId(tenantId)
  return (
    <>
      <GA gaType={GaType.admin} />
      <SwrInitor>
        <AppContextProvider>
          <EventEmitterContextProvider>
            <ProviderContextProvider>
              <ModalContextProvider>
                {!isHuiting && <HeaderWrapper>
                  {isBella ? (<BellaHeader/>) : (<Header />)}
                </HeaderWrapper>}
                {children}
              </ModalContextProvider>
            </ProviderContextProvider>
          </EventEmitterContextProvider>
        </AppContextProvider>
      </SwrInitor>
    </>
  )
}

export default Layout
