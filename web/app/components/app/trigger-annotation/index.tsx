'use client'
import type { FC } from 'react'
import React, { useCallback, useEffect, useState } from 'react'
import cn from '@/utils/classnames'
import WorkflowTrigger from '@/app/components/app/workflow-trigger'
import Loading from '@/app/components/base/loading'
import { useStore as useAppStore } from '@/app/components/app/store'
import { hasEditPermission } from '@/app/components/workflow/hooks/use-workflow'
import { getUserInfo } from '@/utils/getQueryParams'
import { fetchSpaceRole } from '@/service/workflow'
import type { Role } from '@/app/components/workflow/types'

const TriggerAnnotation: FC = () => {
  const appDetail = useAppStore(state => state.appDetail)
  const [role, setRole] = useState<Role | undefined>(undefined)
  const roleInit = useCallback(async () => {
    fetchSpaceRole()
      .then((role) => {
        setRole(role)
      })
      .catch(() => {
        console.log('fetch space role error')
        setRole(undefined) // 设置为 undefined 或一个默认值
      })
  }, [])

  useEffect(() => {
    roleInit()
  }, [])
  const { ucid } = getUserInfo()
  if (!appDetail) {
    return (
      <div className='flex h-full items-center justify-center bg-white'>
        <Loading />
      </div>
    )
  }

  return (
    <div className='pt-4 px-6 h-full flex flex-col'>
      <div className={cn('grow')}>
        <WorkflowTrigger disabled={!hasEditPermission(ucid, appDetail, role)} appDetail={appDetail} />
      </div>
    </div>
  )
}
export default React.memo(TriggerAnnotation)
