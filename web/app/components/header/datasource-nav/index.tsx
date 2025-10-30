'use client'

import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import {
  RiDatabase2Fill,
  RiDatabase2Line,
} from '@remixicon/react'
import { usePathname } from 'next/navigation'
import Nav from '../nav'
import { getTenantId } from '@/utils/getQueryParams'

const DatasourceNav = () => {
  const { t } = useTranslation()
  const pathname = usePathname()
  const firstSeg = pathname.split('/')[1] || ''
  const tenantId = firstSeg || getTenantId() || 'test'

  const matchDatasourceActive = useCallback(({ pathname }: { pathname: string; segment: string | null; segments: string[] }) => {
    return pathname.includes('/datasources')
  }, [])

  return (
    <Nav
      isApp={false}
      icon={<RiDatabase2Line className='w-4 h-4' />}
      activeIcon={<RiDatabase2Fill className='w-4 h-4' />}
      text={t('common.menus.datasources')}
      activeSegment='datasources'
      link={`/${tenantId}/datasources`}
      navs={[]}
      createText=""
      onCreate={() => {}}
      activeMatcher={matchDatasourceActive}
    />
  )
}

export default DatasourceNav
