'use client'

import { useTranslation } from 'react-i18next'
import {
  RiDatabase2Fill,
  RiDatabase2Line,
} from '@remixicon/react'
import Nav from '../nav'

const DatasourceNav = () => {
  const { t } = useTranslation()

  return (
    <Nav
      icon={<RiDatabase2Line className='w-4 h-4' />}
      activeIcon={<RiDatabase2Fill className='w-4 h-4' />}
      text={t('common.menus.datasources')}
      activeSegment='datasources'
      link='/datasources'
      onCreate={() => {}}
    />
  )
}

export default DatasourceNav
