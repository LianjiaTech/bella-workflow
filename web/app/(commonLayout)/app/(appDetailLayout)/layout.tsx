'use client'
import type { FC } from 'react'
import React, { useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAppContext } from '@/context/app-context'
import { setUserInfo } from '@/utils/getQueryParams'

export type IAppDetail = {
  children: React.ReactNode
}

const AppDetail: FC<IAppDetail> = ({ children }) => {
  const searchParams = useSearchParams()
  const userName = searchParams.get('userName') || ''
  const ucid = searchParams.get('ucid') || ''
  const tenantId = searchParams.get('tenantId') || 'test'
  const spaceCode = searchParams.get('spaceCode') || ucid
  setUserInfo(ucid, userName, tenantId, spaceCode)
  const router = useRouter()
  const { isCurrentWorkspaceDatasetOperator } = useAppContext()

  useEffect(() => {
    if (isCurrentWorkspaceDatasetOperator)
      return router.replace('/datasets')
  }, [isCurrentWorkspaceDatasetOperator])

  return (
    <>
      {children}
    </>
  )
}

export default React.memo(AppDetail)
