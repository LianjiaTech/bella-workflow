'use client'
import React from 'react'
import { type Locale } from '@/i18n'
import DevelopMain from '@/app/components/develop'
import { useAppContext } from '@/context/app-context'

export type IDevelopProps = {
  params: { locale: Locale; appId: string }
}
const Develop = ({
  params: { appId },
}: IDevelopProps) => {
  const { tenantConfigFromStore } = useAppContext()
  const features = tenantConfigFromStore?.appConfig?.features

  if (features.develop)
    return <DevelopMain appId={appId} />
  else
    return null
}

export default Develop
