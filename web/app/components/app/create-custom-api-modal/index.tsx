'use client'
import type { MouseEventHandler } from 'react'
import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  RiCloseLine,
} from '@remixicon/react'
import 'react-js-cron/dist/styles.css'
import { useRouter } from 'next/navigation'
import { useContext, useContextSelector } from 'use-context-selector'
import { useDomainList } from '../../workflow/hooks/use-domains'
import { SimpleSelect } from '../../base/select'
import AppsContext, { useAppContext } from '@/context/app-context'
import { ToastContext } from '@/app/components/base/toast'
import { createCustomApi } from '@/service/workflow'
import Modal from '@/app/components/base/modal'
import Button from '@/app/components/base/button'

type CreateTriggerDialogProps = {
  show: boolean
  onSuccess: () => void
  onClose: () => void
  workflowId: string
}

const CreateCustomApiModal = ({ show, onSuccess, onClose, workflowId }: CreateTriggerDialogProps) => {
  const { t } = useTranslation()
  const { push } = useRouter()
  const { notify } = useContext(ToastContext)
  const mutateApps = useContextSelector(AppsContext, state => state.mutateApps)
  const domainList = useDomainList('')

  const [host, setHost] = useState('')
  const [path, setPath] = useState('')

  const { isCurrentWorkspaceEditor } = useAppContext()

  const isCreatingRef = useRef(false)
  const onCreate: MouseEventHandler = useCallback(async () => {
    if (!host.trim()) {
      notify({ type: 'error', message: t('workflow.customApi.hotNotEmpty') })
      return
    }

    if (!path.trim()) {
      notify({ type: 'error', message: t('workflow.customApi.pathNotEmpty') })
      return
    }

    if (!path.startsWith('/')) {
      notify({ type: 'error', message: t('路径需要以斜杆‘/’开头') })
      return
    }

    if (isCreatingRef.current)
      return
    isCreatingRef.current = true
    try {
      const capi = await createCustomApi({
        workflowId,
        host,
        path,
      })
      notify({ type: 'success', message: t('app.newApp.appCreated') })
      onSuccess()
      onClose()
    }
    catch (e) {
      notify({ type: 'error', message: t('app.newApp.appCreateFailed') })
    }
    isCreatingRef.current = false
  }, [host, notify, t, path, onSuccess, onClose, mutateApps, push, isCurrentWorkspaceEditor])

  console.log('domainList', domainList)
  return (
    <Modal
      overflowVisible
      className='!p-0 !max-w-[720px] !w-[720px] rounded-xl'
      isShow={show}
      onClose={() => { }}
    >
      {/* Heading */}
      <div className='shrink-0 flex flex-col h-full bg-white rounded-t-xl'>
        <div className='shrink-0 pl-8 pr-6 pt-6 pb-3 bg-white text-xl rounded-t-xl leading-[30px] font-semibold text-gray-900 z-10'>{t('workflow.customApi.startFromBlank')}</div>
      </div>

      {/* host */}
      <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.customApi.host')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <SimpleSelect
            wrapperClassName='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
            onSelect={(item) => { setHost(item.value.toString()) }}
            items={domainList?.map(d => ({ value: d.domain, name: `${d.domain} ${d.desc}` })) || []}
            placeholder='请选择域名'
          />
        </div>
      </div>

      {/* path */}
      <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.customApi.path')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            value={path}
            onChange={e => setPath(e.target.value)}
            placeholder={t('workflow.customApi.pathPlaceholder') || ''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div>

      <div className='px-8 py-6 flex justify-end'>
        <Button className='mr-2' onClick={onClose}>{t('app.newApp.Cancel')}</Button>
        <Button variant="primary" onClick={onCreate}>{t('app.newApp.Create')}</Button>
      </div>
      <div className='absolute right-6 top-6 p-2 cursor-pointer z-20' onClick={onClose}>
        <RiCloseLine className='w-4 h-4 text-gray-500' />
      </div>
    </Modal>
  )
}

export default CreateCustomApiModal
