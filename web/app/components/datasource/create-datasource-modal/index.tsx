'use client'
import type { MouseEventHandler } from 'react'
import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  RiCloseLine,
} from '@remixicon/react'
import 'react-js-cron/dist/styles.css'
import { useContext } from 'use-context-selector'
import s from './style.module.css'
import cn from '@/utils/classnames'
import { ToastContext } from '@/app/components/base/toast'
import Modal from '@/app/components/base/modal'
import Button from '@/app/components/base/button'
import { createDatasource } from '@/service/common'

type CreateDialogProps = {
  show: boolean
  onSuccess: () => void
  onClose: () => void
  type: string
}

const CreateModal = ({ show, onSuccess, onClose, type }: CreateDialogProps) => {
  const { t } = useTranslation()
  const { notify } = useContext(ToastContext)

  const [config, setConfig] = useState({ dbType: 'mysql', port: 3306 })
  const isCreatingRef = useRef(false)

  const onCreateRdb = useCallback(async () => {
    if (type === 'rdb') {
      if (!config.host) {
        notify({ type: 'error', message: t('datasource.create.hostRequired') })
        return
      }

      if (!config.port) {
        notify({ type: 'error', message: t('datasource.create.portRequired') })
        return
      }

      if (!config.db) {
        notify({ type: 'error', message: t('datasource.create.dbRequired') })
        return
      }

      if (!config.user) {
        notify({ type: 'error', message: t('datasource.create.userRequired') })
        return
      }

      if (!config.password) {
        notify({ type: 'error', message: t('datasource.create.passwordRequired') })
        return
      }
    }

    if (isCreatingRef.current)
      return
    isCreatingRef.current = true
    try {
      const ds = await createDatasource({
        url: '/datasource/rdb/create',
        body: config,
      })
      notify({ type: 'success', message: t('datasource.create.success') })
      onSuccess()
      onClose()
    }
    catch (e) {
      notify({ type: 'error', message: t('datasource.create.failed') })
    }
    isCreatingRef.current = false
  }, [notify, t, onSuccess, onClose, config, type])

  const onCreate: MouseEventHandler = useCallback(async () => {
    if (type === 'rdb')
      onCreateRdb()
  }, [onCreateRdb, type])

  return (
    <Modal
      overflowVisible
      className='!p-0 !max-w-[720px] !w-[720px] rounded-xl'
      isShow={show}
      onClose={() => { }}
    >
      {/* Heading */}
      <div className='shrink-0 flex flex-col h-full bg-white rounded-t-xl'>
        <div className='shrink-0 pl-8 pr-6 pt-6 pb-3 bg-white text-xl rounded-t-xl leading-[30px] font-semibold text-gray-900 z-10'>{t('datasource.add')}</div>
      </div>
      {/* db type */}

      { type === 'rdb' && <div className='py-2 px-8'>
        <div className='py-2 text-sm leading-[20px] font-medium text-gray-900'>{t('datasource.table.dbType')}</div>
        <div className='flex'>

          <div
            className={cn(
              'relative grow box-border w-[158px] mr-2 px-0.5 pt-3 pb-2 flex flex-col items-center justify-center gap-1 rounded-lg border border-gray-100 bg-white text-gray-700 cursor-pointer shadow-xs hover:border-gray-300',
              s['grid-bg-chat'],
              config.dbType === 'mysql' && 'border-[1.5px] border-primary-400 hover:border-[1.5px] hover:border-primary-400',
            )}
            onClick={() => {
              setConfig({ ...config, dbType: 'mysql' })
            }}
          >
            <div className='h-5 text-[13px] font-medium leading-[18px]'>{t('datasource.create.rdb.mysql')}</div>
          </div>

          <div
            className={cn(
              'relative grow box-border w-[158px] px-0.5 pt-3 pb-2 flex flex-col items-center justify-center gap-1 rounded-lg border border-gray-100 text-gray-700 cursor-pointer bg-white shadow-xs hover:border-gray-300',
              s['grid-bg-workflow'],
              config.dbType === 'postgresql' && 'border-[1.5px] border-primary-400 hover:border-[1.5px] hover:border-primary-400',
            )}
            onClick={() => {
              setConfig({ ...config, dbType: 'postgresql' })
            }}
          >
            <div className='h-5 text-[13px] font-medium leading-[18px]'>{t('datasource.create.rdb.postgresql')}</div>
          </div>
        </div>
      </div>
      }

      {/* host */}
      <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('datasource.table.host')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            value={config.host}
            onChange={e => setConfig({ ...config, host: e.target.value })}
            placeholder={t('datasource.create.hostPlaceholder') || ''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div>

      {/* port */}
      <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('datasource.table.port')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            value={config.port}
            onChange={e => setConfig({ ...config, port: e.target.value })}
            placeholder={''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div>

      {/* dbType */}
      { type === 'rdb' && <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('datasource.table.db')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            value={config.db}
            onChange={e => setConfig({ ...config, db: e.target.value })}
            placeholder={''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div> }

      {/* user */}
      { type === 'rdb' && <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('datasource.table.user')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            name='username'
            value={config.user}
            onChange={e => setConfig({ ...config, user: e.target.value })}
            placeholder={''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div> }

      {/* port */}
      { type === 'rdb' && <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('datasource.table.password')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            type='password'
            value={config.password}
            onChange={e => setConfig({ ...config, password: e.target.value })}
            placeholder={''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div> }

      {/* params */}
      { type === 'rdb' && <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('datasource.table.params')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            value={config.params}
            onChange={e => setConfig({ ...config, params: e.target.value })}
            placeholder={t('datasource.create.paramsPlaceholder') || ''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div> }

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

export default CreateModal
