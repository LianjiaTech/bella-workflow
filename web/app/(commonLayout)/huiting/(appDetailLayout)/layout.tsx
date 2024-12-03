'use client'
import type { FC } from 'react'
import React from 'react'
import { useSearchParams } from 'next/navigation'
import { getTenantId, setUserInfo } from '@/utils/getQueryParams'

export type IAppDetail = {
  children: React.ReactNode
}

const AppDetail: FC<IAppDetail> = ({ children }) => {
  const searchParams = useSearchParams()
  const userName = searchParams.get('userName') || ''
  const ucid = searchParams.get('ucid') || ''
  const tenantId = getTenantId()
  const spaceCode = searchParams.get('spaceCode') || ucid
  setUserInfo(ucid, userName, tenantId, spaceCode)
  return (
    <>
      {children}
    </>
  )
}

export default React.memo(AppDetail)
