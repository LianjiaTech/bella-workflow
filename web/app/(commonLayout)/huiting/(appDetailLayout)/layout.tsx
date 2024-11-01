'use client'
import type { FC } from 'react'
import React from 'react'
import { useSearchParams } from 'next/navigation'
import { setUserInfo } from '@/utils/getQueryParams'

export type IAppDetail = {
  children: React.ReactNode
}

const AppDetail: FC<IAppDetail> = ({ children }) => {
  const searchParams = useSearchParams()
  const userName = searchParams.get('userName') || ''
  const ucid = searchParams.get('ucid') || ''
  const tenantId = searchParams.get('tenantId') || 'TENT-d815410c-f9db-459e-b4ab-67a52d8e63ce'
  const spaceCode = searchParams.get('spaceCode') || ucid
  setUserInfo(ucid, userName, tenantId, spaceCode)
  return (
    <>
      {children}
    </>
  )
}

export default React.memo(AppDetail)
