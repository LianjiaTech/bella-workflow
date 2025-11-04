'use client'

import React from 'react'
import Main from '@/app/components/app/capi-annotation'

import { useAppContext } from '@/context/app-context'
const Capis = () => {
  const { tenantConfigFromStore } = useAppContext()
  const features = tenantConfigFromStore?.appConfig?.features
  if (features.customApi)
    return <Main />
  else
    return null
}

export default Capis
