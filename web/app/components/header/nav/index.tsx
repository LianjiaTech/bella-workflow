'use client'

import React, { useState } from 'react'
import Link from 'next/link'
import { usePathname, useSelectedLayoutSegment } from 'next/navigation'
import type { INavSelectorProps } from './nav-selector'
import NavSelector from './nav-selector'
import classNames from '@/utils/classnames'
import { ArrowNarrowLeft } from '@/app/components/base/icons/src/vender/line/arrows'
import { useStore as useAppStore } from '@/app/components/app/store'

type INavProps = {
  icon: React.ReactNode
  activeIcon?: React.ReactNode
  text: string
  activeSegment?: string | string[]
  link: string
  isApp: boolean
  activeMatcher?: (payload: { pathname: string; segment: string | null; segments: string[] }) => boolean
} & INavSelectorProps

const Nav = ({
  icon,
  activeIcon,
  text,
  activeSegment,
  link,
  curNav,
  navs,
  createText,
  onCreate,
  onLoadmore,
  isApp,
  activeMatcher,
}: INavProps) => {
  const setAppDetail = useAppStore(state => state.setAppDetail)
  const [hovered, setHovered] = useState(false)
  const segment = useSelectedLayoutSegment()
  const pathname = usePathname()

  const checkActive = () => {
    const segments = pathname.split('/').filter(Boolean)

    if (activeMatcher)
      return activeMatcher({ pathname, segment, segments })

    const segmentsToMatch = (Array.isArray(activeSegment) ? activeSegment : activeSegment ? [activeSegment] : []).filter(Boolean) as string[]

    if (segment && segmentsToMatch.includes(segment))
      return true

    return segmentsToMatch.some(seg => segments.includes(seg))
  }

  const isActived = checkActive()

  return (
    <div className={`
      flex items-center h-8 mr-0 sm:mr-3 px-0.5 rounded-xl text-sm shrink-0 font-medium
      ${isActived && 'bg-components-main-nav-nav-button-bg-active shadow-md font-semibold'}
      ${!curNav && !isActived && 'hover:bg-components-main-nav-nav-button-bg-hover'}
    `}>
      <Link href={link}>
        <div
          onClick={() => setAppDetail()}
          className={classNames(`
            flex items-center h-7 px-2.5 cursor-pointer rounded-[10px]
            ${isActived ? 'text-components-main-nav-nav-button-text-active' : 'text-components-main-nav-nav-button-text'}
            ${curNav && isActived && 'hover:bg-components-main-nav-nav-button-bg-active-hover'}
          `)}
          onMouseEnter={() => setHovered(true)}
          onMouseLeave={() => setHovered(false)}
        >
          <div className='mr-2'>
            {
              (hovered && curNav)
                ? <ArrowNarrowLeft className='w-4 h-4' />
                : isActived
                  ? activeIcon
                  : icon
            }
          </div>
          {text}
        </div>
      </Link>
      {
        curNav && isActived && (
          <>
            <div className='font-light text-gray-300 '>/</div>
            <NavSelector
              isApp={isApp}
              curNav={curNav}
              navs={navs}
              createText={createText}
              onCreate={onCreate}
              onLoadmore={onLoadmore}
            />
          </>
        )
      }
    </div>
  )
}

export default Nav
