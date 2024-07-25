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
  const tenantId = searchParams.get('tenantId') || '04633c4f-8638-43a3-a02e-af23c29f821f'
  setUserInfo(ucid, userName, tenantId)
  return (
    <>
      {children}
    </>
  )
}

export default React.memo(AppDetail)
