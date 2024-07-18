'use client'
import React from 'react'
import type { ReactNode } from 'react'
import { useSearchParams } from 'next/navigation'
import SwrInitor from '@/app/components/swr-initor'
import { AppContextProvider } from '@/context/app-context'
import GA, { GaType } from '@/app/components/base/ga'
import HeaderWrapper from '@/app/components/header/HeaderWrapper'
import Header from '@/app/components/header'
import { EventEmitterContextProvider } from '@/context/event-emitter'
import { ProviderContextProvider } from '@/context/provider-context'
import { ModalContextProvider } from '@/context/modal-context'

const Layout = ({ children }: { children: ReactNode }) => {
  const searchParams = useSearchParams()
  const userName = searchParams.get('userName') || ''
  const ucid = searchParams.get('ucid') || ''
  globalThis.localStorage?.setItem('userName', userName)
  globalThis.localStorage?.setItem('ucid', ucid)
  return (
    <>
      <GA gaType={GaType.admin} />
      <SwrInitor>
        <AppContextProvider>
          <EventEmitterContextProvider>
            <ProviderContextProvider>
              <ModalContextProvider>
                <HeaderWrapper>
                  <Header />
                </HeaderWrapper>
                {children}
              </ModalContextProvider>
            </ProviderContextProvider>
          </EventEmitterContextProvider>
        </AppContextProvider>
      </SwrInitor>
    </>
  )
}

// export const metadata = {
//   title: 'Bella工作流',
// }

export default Layout
