'use client'
import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { TenantSessionManager } from '@/utils/tenant-session'
import Loading from '@/app/components/base/loading'

const HomePage = () => {
  const router = useRouter()

  useEffect(() => {
    // 重定向到默认租户的apps页面
    const defaultTenant = TenantSessionManager.getDefaultTenant()
    router.replace(`/tenant/${defaultTenant}/apps`)
  }, [router])

  return (
    <div className="flex flex-col justify-center min-h-screen py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Loading type='area' />
        <div className="mt-10 text-center">
          <p>Redirecting to apps...</p>
        </div>
      </div>
    </div>
  )
}

export default HomePage
