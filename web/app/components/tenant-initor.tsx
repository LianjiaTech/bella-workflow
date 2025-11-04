'use client'

import { useEffect } from 'react'
import { usePathname } from 'next/navigation'
import { TenantSessionManager } from '@/utils/tenant-session'

const TenantInitor = ({
  children,
}: { children: React.ReactElement }) => {
  const pathname = usePathname()

  useEffect(() => {
    // 页面加载时从URL提取租户信息并存储到sessionStorage
    TenantSessionManager.extractAndSetFromUrl(pathname)
  }, [pathname])

  return children
}

export default TenantInitor
