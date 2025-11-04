'use client'

import React from 'react'
import Main from '@/app/components/app/trigger-annotation'
import { useAppContext } from '@/context/app-context'

const Logs = () => {
  const { tenantConfigFromStore } = useAppContext()
  const features = tenantConfigFromStore?.appConfig?.features
  if (features.develop)
    return <Main />
  else
    return null
}

export default Logs
