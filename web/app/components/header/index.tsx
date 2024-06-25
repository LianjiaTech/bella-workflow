'use client'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useClickAway } from 'ahooks'
import Image from 'next/image'
import { useSearchParams } from 'next/navigation'
import { useContext } from 'use-context-selector'
import edit from './assets/edit.png'
import back from './assets/back.png'
import { ToastContext } from '@/app/components/base/toast'
import { fetchAppDetail, updateAppInfo } from '@/service/apps'
import { fetchDraftInfo } from '@/service/v1'
import { getQueryParams } from '@/utils/getQueryParams'
import Modal from '@/app/components/base/modal'
import Button from '@/app/components/base/button'

const Header = () => {
  const { t } = useTranslation()
  const searchParams = useSearchParams()
  const timstamp = new Date().getTime()
  const _workflowName = `未命名工作流_${timstamp}}`
  const [workflowName, setWorkflowName] = useState('')
  const [isEdit, setIsEdit] = useState(false)
  const inputRef = useRef(null)
  const { notify } = useContext(ToastContext)
  const [isOpen, setIsOpen] = useState(false)

  // 获取当前工作流的appid
  const getAppId = () => {
    const pathname = window.location.pathname
    const pathnameSplitArr = pathname.split('/')
    return pathnameSplitArr[2]
  }
  const appID = getAppId()
  useEffect(() => {
    console.log(window.location, 'dddd')
    fetchAppDetail({ url: '/apps', id: appID }).then((res) => {
      res?.name && setWorkflowName(res?.name)
    })
  }, [])
  // 编辑工作流名称
  const handleEdit = (flag) => {
    setIsEdit(flag)
  }
  // 退出工作流
  const exitWorkflow = () => {
    const bellaId = getQueryParams('bellaId')
    // 创建or编辑
    const bellaUrl = `http://example.com/#/createagent?applicationId=${bellaId}&workflowId=${getAppId()}`
    window.parent.location.href = bellaUrl
  }
  // 返回bella
  const goBackBella = () => {
    const page = 1
    const limit = 1
    fetchDraftInfo(getAppId(), page, limit).then((res) => {
      console.log('是否发布', res)
      const { data } = res
      data[0]?.version === 0 ? setIsOpen(true) : exitWorkflow()
    })
  }

  // 继续编辑工作流
  const continueExitWorkflow = () => {
    setIsOpen(false)
  }
  useClickAway(async () => {
    if (workflowName.trim() === '') {
      notify({
        type: 'error',
        message: '工作流名称不能为空',
      })
      return
    }
    try {
      const updateName = await updateAppInfo({
        appID,
        name: workflowName,
        icon: '',
        icon_background: '',
        description: '',
      })
      handleEdit(false)
    }
    catch (e) {
      console.log('更新工作流名称失败', e)
    }
  }, inputRef)
  return (
    <div className="flex flex-1 items-center justify-between px-4">
      <div className="flex items-center" ref={inputRef}>
        <Image
          className="cursor-pointer"
          src={back}
          width={24}
          onClick={goBackBella}
        />
        {isEdit
          ? (
            <input
              value={workflowName}
              onChange={e => setWorkflowName(e.target.value)}
              className="grow h-8 px-3 mx-4 min-w-[200px] text-sm font-normal bg-white rounded-lg border outline-none appearance-none caret-primary-600 placeholder:text-gray-400 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs"
            />
          )
          : (
            <span className="px-4">{workflowName}</span>
          )}

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
    </div>
  )
}
export default Header
