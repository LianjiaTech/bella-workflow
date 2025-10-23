'use client'
import { useCallback, useEffect, useMemo } from 'react'
import Link from 'next/link'
import { useBoolean } from 'ahooks'
import { usePathname, useSelectedLayoutSegment } from 'next/navigation'
import { Bars3Icon } from '@heroicons/react/20/solid'
import HeaderBillingBtn from '../billing/header-billing-btn'
import AccountDropdown from './account-dropdown'
import WorkplaceSelector from './space-dropdown/workplace-selector'
import AppNav from './app-nav'
import EnvNav from './env-nav'
import DatasourceNav from './datasource-nav'
import { WorkspaceProvider } from '@/context/workspace-context'
import { useAppContext } from '@/context/app-context'
import LogoSite from '@/app/components/base/logo/logo-site'
import useBreakpoints, { MediaType } from '@/hooks/use-breakpoints'
import { useProviderContext } from '@/context/provider-context'
import { useModalContext } from '@/context/modal-context'

// const navClassName = `
//   flex items-center relative mr-0 sm:mr-3 px-3 h-8 rounded-xl
//   font-medium text-sm
//   cursor-pointer
// `

const Header = () => {
  const { isCurrentWorkspaceEditor, isCurrentWorkspaceDatasetOperator, tenantConfigFromStore } = useAppContext()

  const selectedSegment = useSelectedLayoutSegment()
  const media = useBreakpoints()
  const isMobile = media === MediaType.mobile
  const [isShowNavMenu, { toggle, setFalse: hideNavMenu }] = useBoolean(false)
  const { enableBilling, plan } = useProviderContext()
  const { setShowPricingModal, setShowAccountSettingModal } = useModalContext()
  const isFreePlan = plan.type === 'sandbox'
  const pathname = usePathname()
  const firstSeg = pathname.split('/')[1]
  const secondSeg = pathname.split('/')[2]
  const isGlobalSection = () => firstSeg === 'app' || firstSeg === 'apps' || firstSeg === 'datasources'

  const handlePlanClick = useCallback(() => {
    if (isFreePlan)
      setShowPricingModal()
    else
      setShowAccountSettingModal({ payload: 'billing' })
  }, [isFreePlan, setShowAccountSettingModal, setShowPricingModal])

  useEffect(() => {
    hideNavMenu()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedSegment])

  const appHeaderConfig = useMemo(() => {
    if (isGlobalSection() || secondSeg === 'datasources') {
      return {
        showWorkspaceProvider: true,
        showWorkroomButton: true,
        showDataSourceButton: true,
        showUserInfo: true,
        iconConfig: { clickable: true },
      }
    }

    return tenantConfigFromStore?.appConfig?.appHeader || {
      showWorkspaceProvider: false,
      showWorkroomButton: false,
      showDataSourceButton: false,
      showUserInfo: false,
      iconConfig: { clickable: false },
    }
  }, [isGlobalSection, secondSeg, tenantConfigFromStore?.appConfig?.appHeader])

  return (
    <div className='flex flex-1 items-center justify-between px-4'>
      <div className='flex items-center'>
        {isMobile && <div
          className='flex items-center justify-center h-8 w-8 cursor-pointer'
          onClick={toggle}
        >
          <Bars3Icon className="h-4 w-4 text-gray-500" />
        </div>}
        {!isMobile && <>
          {appHeaderConfig?.iconConfig?.clickable
            ? <Link href="/apps" className='flex items-center mr-4'>
              <LogoSite className='object-contain' />
            </Link>
            : <LogoSite className='object-contain' />
          }
          {appHeaderConfig.showWorkspaceProvider
            && <WorkspaceProvider>
              <WorkplaceSelector />
            </WorkspaceProvider>
          }
        </>}
      </div>
      {isMobile && (
        <div className='flex'>
          {appHeaderConfig?.iconConfig?.clickable
            ? <Link href="/apps" className='flex items-center mr-4'>
              <LogoSite />
            </Link>
            : <LogoSite />
          }
        </div>
      )}
      {!isMobile && (
        <div className='flex items-center'>
          {/* isCurrentWorkspaceDatasetOperator 和 isCurrentWorkspaceEditor其实是没有生效的,Appnav和DataSourceNav是常态存在的 */}
          {(!isCurrentWorkspaceDatasetOperator && appHeaderConfig.showWorkroomButton) && <AppNav />}
          {((isCurrentWorkspaceEditor || isCurrentWorkspaceDatasetOperator) && appHeaderConfig.showDataSourceButton) && <DatasourceNav />}
        </div>
      )}
      <div className='flex items-center flex-shrink-0'>
        <EnvNav />
        {enableBilling && (
          <div className='mr-3 select-none'>
            <HeaderBillingBtn onClick={handlePlanClick} />
          </div>
        )}
        {appHeaderConfig.showUserInfo
          && <WorkspaceProvider>
            <AccountDropdown isMobile={isMobile} />
          </WorkspaceProvider>
        }
      </div>
      {(isMobile && isShowNavMenu) && (
        <div className='w-full flex flex-col p-2 gap-y-1'>
          {(!isCurrentWorkspaceDatasetOperator && appHeaderConfig.showWorkroomButton) && <AppNav />}
          {(isCurrentWorkspaceEditor || isCurrentWorkspaceDatasetOperator) && appHeaderConfig.showDataSourceButton && <DatasourceNav />}
        </div>
      )}
    </div>
  )
}
export default Header
