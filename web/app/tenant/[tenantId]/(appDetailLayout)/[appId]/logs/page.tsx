'use client'

import React from 'react'
import Main from '@/app/components/app/log-annotation'
import { PageType } from '@/app/components/app/configuration/toolbox/annotation/type'
import { useAppContext } from '@/context/app-context'

const Logs = () => {
  const { tenantConfigFromStore } = useAppContext()
  const features = tenantConfigFromStore?.appConfig?.features

  if (features.logs)
    return <Main pageType={PageType.log} />
  else
    return null
}

export default Logs
