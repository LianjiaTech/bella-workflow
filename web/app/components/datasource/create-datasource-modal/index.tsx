'use client'
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
import Input from '@/app/components/base/input'
import { createDatasource } from '@/service/common'
import { useAppContext } from '@/context/app-context'

type FormData = {
  [key: string]: any
}

type FormField = {
  key: string
  label: string
  placeholder?: string
  type?: 'text' | 'password'
  required?: boolean
}

type FormSection = {
  type: 'input' | 'radio'
  fields: FormField[]
  options?: {
    value: string
    label: string
    icon?: string
  }[]
}

const FORM_CONFIG: Record<string, FormSection[]> = {
  rdb: [
    {
      type: 'radio',
      fields: [{
        key: 'dbType',
        label: 'datasource.table.dbType',
      }],
      options: [
        {
          value: 'mysql',
          label: 'datasource.create.rdb.mysql',
        },
        {
          value: 'postgresql',
          label: 'datasource.create.rdb.postgresql',
        },
      ],
    },
    {
      type: 'input',
      fields: [
        {
          key: 'host',
          label: 'datasource.table.host',
          placeholder: 'datasource.create.hostPlaceholder',
          required: true,
        },
        {
          key: 'port',
          label: 'datasource.table.port',
          required: true,
        },
        {
          key: 'db',
          label: 'datasource.table.db',
          required: true,
        },
        {
          key: 'user',
          label: 'datasource.table.user',
          required: true,
        },
        {
          key: 'password',
          label: 'datasource.table.password',
          type: 'password',
          required: true,
        },
        {
          key: 'params',
          label: 'datasource.table.params',
          placeholder: 'datasource.create.paramsPlaceholder',
        },
      ],
    },
  ],
  redis: [
    {
      type: 'input',
      fields: [
        {
          key: 'host',
          label: 'datasource.table.host',
          placeholder: 'datasource.create.hostPlaceholder',
          required: true,
        },
        {
          key: 'port',
          label: 'datasource.table.port',
          required: true,
        },
        {
          key: 'user',
          label: 'datasource.table.user',
          placeholder: 'datasource.create.redis.userPlaceholder',
        },
        {
          key: 'password',
          label: 'datasource.table.password',
          type: 'password',
        },
        {
          key: 'db',
          label: 'datasource.table.db',
          placeholder: 'datasource.create.redis.dbPlaceholder',
        },
      ],
    },
  ],
  kafka: [
    {
      type: 'radio',
      fields: [{
        key: 'type',
        label: 'datasource.create.kafka_type',
      }],
      options: [
        {
          value: 'consumer',
          label: 'datasource.create.kafka.consumer',
        },
        {
          value: 'producer',
          label: 'datasource.create.kafka.producer',
        },
      ],
    },
    {
      type: 'input',
      fields: [
        {
          key: 'server',
          label: 'datasource.create.kafka_server',
          placeholder: 'datasource.create.kafka_server_placeholder',
          required: true,
        },
        {
          key: 'name',
          label: 'datasource.create.kafka_name',
          placeholder: 'datasource.create.kafka_name_placeholder',
        },
        {
          key: 'topic',
          label: 'datasource.create.kafka_topic',
          placeholder: 'datasource.create.kafka_topic_placeholder',
          required: true,
        },
        {
          key: 'msgSchema',
          label: 'datasource.create.kafka_msgSchema',
          placeholder: 'datasource.create.kafka_msgSchema_placeholder',
        },
      ],
    },
  ],
}

type CreateDialogProps = {
  show: boolean
  onSuccess: () => void
  onClose: () => void
  type: 'rdb' | 'kafka' | 'redis'
}

const CreateModal = ({ show, onSuccess, onClose, type }: CreateDialogProps) => {
  const { t } = useTranslation()
  const { notify } = useContext(ToastContext)
  const { userProfile } = useAppContext()
  const [pending, setPending] = useState(false)
  const isCreatingRef = useRef(false)

  const [formData, setFormData] = useState<FormData>({
    ...(type === 'rdb'
      ? { dbType: 'mysql', port: 3306 }
      : type === 'redis'
        ? { port: 6379, db: 0, user: '', password: '' }
        : { type: 'consumer', topic: '', msgSchema: '', name: '' }),
  })

  const handleClose = () => {
    setFormData({
      ...(type === 'rdb'
        ? { dbType: 'mysql', port: 3306 }
        : type === 'redis'
          ? { port: 6379, db: 0, user: '', password: '' }
          : { type: 'consumer', topic: '', msgSchema: '', name: '' }),
    })
    onClose()
  }

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }))
  }

  const validateForm = () => {
    const config = FORM_CONFIG[type]
    const requiredFields = config.flatMap(section =>
      section.fields.filter((field) => {
        // 对于 Kafka，topic 字段只在消费者类型时是必需的
        if (type === 'kafka' && field.key === 'topic')
          return formData.type === 'consumer'

        return field.required
      }).map(field => field.key),
    )

    for (const field of requiredFields) {
      if (!formData[field]) {
        // 使用完整的错误消息 key，根据数据源类型区分
        notify({ type: 'error', message: t(`datasource.create.${type}_${field}_required`) })
        return false
      }
    }
    return true
  }

  const handleSubmit = useCallback(async () => {
    if (!validateForm())
      return

    if (isCreatingRef.current)
      return
    isCreatingRef.current = true

    try {
      setPending(true)
      const submitData = { ...formData }
      // 如果是生产者，清空 topic 和 msgSchema
      if (type === 'kafka' && formData.type === 'producer') {
        submitData.topic = ''
        submitData.msgSchema = ''
      }

      await createDatasource({
        url: `/datasource/${type}/create`,
        body: submitData,
      })

      notify({ type: 'success', message: t('datasource.create.success') })
      onSuccess()
      onClose()
    }
    catch (e) {
      notify({ type: 'error', message: t('datasource.create.failed') })
    }
    finally {
      setPending(false)
      isCreatingRef.current = false
    }
  }, [formData, notify, t, onSuccess, onClose, type])

  const renderFormSection = (section: FormSection) => {
    if (section.type === 'radio') {
      return (
        <div className='py-2 px-8' key={section.fields[0].key}>
          <div className='py-2 text-sm leading-[20px] font-medium text-gray-900'>{t(section.fields[0].label)}</div>
          <div className='flex'>
            {section.options?.map((option, index) => (
              <div
                key={option.value}
                className={cn(
                  'relative grow box-border w-[158px] px-0.5 pt-3 pb-2 flex flex-col items-center justify-center gap-1 rounded-lg border border-gray-100 text-gray-700 cursor-pointer bg-white shadow-xs hover:border-gray-300',
                  index === 0 && 'mr-2',
                  index === 0 ? s['grid-bg-chat'] : s['grid-bg-workflow'],
                  formData[section.fields[0].key] === option.value && 'border-[1.5px] border-primary-400 hover:border-[1.5px] hover:border-primary-400',
                )}
                onClick={() => handleInputChange(section.fields[0].key, option.value)}
              >
                <div className='h-5 text-[13px] font-medium leading-[18px]'>{t(option.label)}</div>
              </div>
            ))}
          </div>
        </div>
      )
    }

    return section.fields.map((field) => {
      // 如果是 Kafka 生产者，不显示 topic 和 msgSchema 字段
      if (type === 'kafka' && formData.type === 'producer' && (field.key === 'topic' || field.key === 'msgSchema'))
        return null

      return (
        <div className='pt-2 px-8' key={field.key}>
          <div className='text-sm leading-[20px] font-medium text-gray-900'>{t(field.label)}</div>
          <div className='mt-2'>
            <Input
              className='!h-9'
              value={formData[field.key] || ''}
              onChange={value => handleInputChange(field.key, value.target.value)}
              placeholder={field.placeholder ? t(field.placeholder) : ''}
              type={field.type || 'text'}
            />
          </div>
        </div>
      )
    })
  }

  return (
    <Modal
      overflowVisible
      className='!p-0 !max-w-[720px] !w-[720px] rounded-xl'
      isShow={show}
      onClose={() => { }}
    >
      <div className='shrink-0 flex flex-col h-full bg-white rounded-t-xl'>
        <div className='shrink-0 pl-8 pr-6 pt-6 pb-3 bg-white text-xl rounded-t-xl leading-[30px] font-semibold text-gray-900 z-10'>
          {t('datasource.add')}
        </div>
      </div>

      {FORM_CONFIG[type].map(section => renderFormSection(section))}

      <div className='px-8 py-6 flex justify-end'>
        <Button className='mr-2' onClick={handleClose}>{t('app.newApp.Cancel')}</Button>
        <Button variant="primary" loading={pending} onClick={handleSubmit}>{t('app.newApp.Create')}</Button>
      </div>
      <div className='absolute right-6 top-6 p-2 cursor-pointer z-20' onClick={onClose}>
        <RiCloseLine className='w-4 h-4 text-gray-500' />
      </div>
    </Modal>
  )
}

export default CreateModal
