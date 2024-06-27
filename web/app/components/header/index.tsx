'use client'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useShallow } from 'zustand/react/shallow'
import { useTranslation } from 'react-i18next'
import Image from 'next/image'
import { useSearchParams } from 'next/navigation'
import { useContext, useContextSelector } from 'use-context-selector'
import edit from './assets/edit.png'
import back from './assets/back.png'
import { ToastContext } from '@/app/components/base/toast'
import { fetchAppDetail, updateAppInfo } from '@/service/apps'
import { fetchDraftInfo } from '@/service/v1'
import { getQueryParams } from '@/utils/getQueryParams'
import Modal from '@/app/components/base/modal'
import Button from '@/app/components/base/button'
import { useStore as useAppStore } from '@/app/components/app/store'
import CreateAppModal from '@/app/components/explore/create-app-modal'
import type { CreateAppModalProps } from '@/app/components/explore/create-app-modal'
import AppsContext from '@/context/app-context'

const Header = () => {
  const { t } = useTranslation()
  const searchParams = useSearchParams()
  const timstamp = new Date().getTime()
  const _workflowName = `未命名工作流_${timstamp}}`
  const [workflowName, setWorkflowName] = useState('')
  const [isEdit, setIsEdit] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const inputRef = useRef(null)
  const { notify } = useContext(ToastContext)
  const [isOpen, setIsOpen] = useState(false)
  // const appDetail = useAppStore(state => state.appDetail)
  const { appDetail, setAppDetail } = useAppStore(useShallow(state => ({
    appDetail: state.appDetail,
    setAppDetail: state.setAppDetail,
  })))
  // 获取当前工作流的appid
  const getAppId = () => {
    const pathname = window.location.pathname
    const pathnameSplitArr = pathname.split('/')
    return pathnameSplitArr[2]
  }
  const mutateApps = useContextSelector(AppsContext, state => state.mutateApps)
  const appID = getAppId()
  useEffect(() => {
    fetchAppDetail({ url: '/apps', id: appID }).then((res) => {
      res?.name && setWorkflowName(res?.name)
    })
  }, [])
  useEffect(() => {
    setWorkflowName(appDetail?.name)
  }, [appDetail])
  const onEdit: CreateAppModalProps['onConfirm'] = useCallback(async ({
    name,
    icon,
    icon_background,
    description,
  }) => {
    if (!appDetail)
      return
    if (description === '' || description.trim() === '') {
      notify({ type: 'error', message: '描述不能为空' })
      return
    }
    try {
      const app = await updateAppInfo({
        appID: appDetail.id,
        name,
        icon,
        icon_background,
        description,
      })
      setShowEditModal(false)
      notify({
        type: 'success',
        message: t('app.editDone'),
      })
      setAppDetail(app)
      mutateApps()
    }
    catch (e) {
      notify({ type: 'error', message: t('app.editFailed') })
    }
  }, [appDetail, mutateApps, notify, setAppDetail, t])
  // 编辑工作流名称
  const handleEdit = (flag: any) => {
    setIsEdit(flag)
    setShowEditModal(true)
  }
  // 退出工作流
  const exitWorkflow = () => {
    const bellaId = getQueryParams('bellaId')
    const appId = getAppId()

    // 根据环境设置bellaHost
    const bellaHost = window.location.hostname.includes('example.com')
      ? 'http://example.com'
      : 'http://example.com:5173'

    // 构建URL
    let bellaUrl = `${bellaHost}/#/createagent?workflowId=${appId}&workflowName=${workflowName}`
    if (bellaId !== 'undefined')
      bellaUrl += `&applicationId=${bellaId}`

    // 重定向
    window.parent.location.href = bellaUrl
  }
  // 返回bella
  const goBackBella = () => {
    const page = 1
    const limit = 1
    fetchDraftInfo(getAppId(), page, limit).then((res) => {
      const { data } = res
      data[0]?.version === 0 ? setIsOpen(true) : exitWorkflow()
    })
  }

  // 继续编辑工作流
  const continueExitWorkflow = () => {
    setIsOpen(false)
  }
  // useClickAway(async () => {
  //   if (workflowName.trim() === '') {
  //     notify({
  //       type: 'error',
  //       message: '工作流名称不能为空',
  //     })
  //     return
  //   }
  //   try {
  //     const updateName = await updateAppInfo({
  //       appID,
  //       name: workflowName,
  //       icon: '',
  //       icon_background: '',
  //       description: '',
  //     })
  //     handleEdit(false)
  //   }
  //   catch (e) {
  //     console.log('更新工作流名称失败', e)
  //   }
  // }, inputRef)
  return (
    <div className="flex flex-1 items-center justify-between px-4">
      <div className="flex items-center" ref={inputRef}>
        <Image
          className="cursor-pointer"
          src={back}
          width={24}
          onClick={goBackBella}
        />
        {/* {isEdit
          ? (
            <input
              value={workflowName}
              onChange={e => setWorkflowName(e.target.value)}
              className="grow h-8 px-3 mx-4 min-w-[200px] text-sm font-normal bg-white rounded-lg border outline-none appearance-none caret-primary-600 placeholder:text-gray-400 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs"
            />
          )
          : (

          )} */}
        <span className="px-4">{workflowName}</span>
        <Image
          className="cursor-pointer"
          onClick={() => handleEdit(true)}
          src={edit}
          width={18}
        />
      </div>
      <Modal
        isShow={isOpen}
        onClose={() => {}}
        wrapperClassName="z-40"
        className="relative !max-w-[480px] px-8"
      >
        <div className="mb-9">
          当前工作流还未成功发布，可能会影响智能体的正常运行，是否继续编辑工作流？
        </div>
        <div className="flex justify-end">
          <Button className="w-24 mr-2" onClick={exitWorkflow}>
            退出
          </Button>
          <Button
            className="w-24 "
            type="primary"
            onClick={continueExitWorkflow}
          >
            继续
          </Button>
        </div>
      </Modal>
      {/* 编辑工作流信息，showEditModal可以确保数据更新后页面也更新 */}
      {
        showEditModal && <CreateAppModal
          isEditModal
          appIcon={appDetail?.icon}
          appIconBackground={appDetail?.icon_background}
          appName={workflowName}
          appDescription={appDetail?.description}
          show={showEditModal}
          onConfirm={onEdit}
          onHide={() => {}}
        />
      }
    </div>
  )
}
export default Header
