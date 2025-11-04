'use client'

import { useTranslation } from 'react-i18next'
import {
  RiDatabase2Fill,
  RiDatabase2Line,
} from '@remixicon/react'
import { usePathname } from 'next/navigation'
import Nav from '../nav'
import { getTenantId } from '@/utils/getQueryParams'
import { getDatasourcesRoute } from '@/utils/tenant-routes'
import { TenantSessionManager } from '@/utils/tenant-session'

const DatasourceNav = () => {
  const { t } = useTranslation()
  const pathname = usePathname()

  // 从租户路径中提取tenantId
  const tenantMatch = pathname.match(/^\/tenant\/([^\/]+)/)
  const tenantId = tenantMatch ? tenantMatch[1] : (getTenantId() || TenantSessionManager.getCurrentTenant())

  return (
    <Nav
      isApp={false}
      icon={<RiDatabase2Line className='w-4 h-4' />}
      activeIcon={<RiDatabase2Fill className='w-4 h-4' />}
      text={t('common.menus.datasources')}
      activeSegment='datasources'
      link={getDatasourcesRoute(undefined, tenantId)}
      navs={[]}
      createText=""
      onCreate={() => {}}
    />
  )
}

export default DatasourceNav
