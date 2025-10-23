'use client'
import type { FC } from 'react'
import React from 'react'
import { useSearchParams } from 'next/navigation'
import { setUserInfo } from '@/utils/getQueryParams'

export type IAppDetail = {
  children: React.ReactNode
  params: { tenantId: string }
}

const AppDetail: FC<IAppDetail> = ({ children, params }) => {
  const searchParams = useSearchParams()
  // const router = useRouter()
  const userName = searchParams.get('userName') || ''
  const ucid = searchParams.get('ucid') || ''
  const { tenantId } = params
  const spaceCode = searchParams.get('spaceCode') || ucid

  // 设置用户信息，使用动态租户ID
  setUserInfo(ucid, userName, tenantId, spaceCode)

  return (
    <>
      {children}
    </>
  )
}

export default React.memo(AppDetail)
