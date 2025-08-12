'use client'
import type { MouseEventHandler } from 'react'
import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  RiCloseLine,
} from '@remixicon/react'
import { Cron } from 'react-js-cron'
import 'react-js-cron/dist/styles.css'
import { useRouter } from 'next/navigation'
import { useContext, useContextSelector } from 'use-context-selector'
import Editor, { loader } from '@monaco-editor/react'
import { useDatasourceList } from '../../workflow/hooks/use-datasource'
import { DEFAULT_LOCALE_CHS } from './cron-locale'
import s from './style.module.css'
import cn from '@/utils/classnames'
import AppsContext, { useAppContext } from '@/context/app-context'
import { ToastContext } from '@/app/components/base/toast'
import { createSchedulingTrigger, debugPublishedWorkflow } from '@/service/workflow'
import Modal from '@/app/components/base/modal'
import Button from '@/app/components/base/button'
import TooltipPlus from '@/app/components/base/tooltip-plus'
import { SimpleSelect } from '@/app/components/base/select'

// load file from local instead of cdn https://github.com/suren-atoyan/monaco-react/issues/482
loader.config({ paths: { vs: '/vs' } })

type CreateTriggerDialogProps = {
  show: boolean
  onSuccess: () => void
  onClose: () => void
  workflowId: string
}

const CreateTriggerModal = ({ show, onSuccess, onClose, workflowId }: CreateTriggerDialogProps) => {
  const { t } = useTranslation()
  const { push } = useRouter()
  const { notify } = useContext(ToastContext)
  const mutateApps = useContextSelector(AppsContext, state => state.mutateApps)
  const { data: datasourceList, mutate: mutateDatasource, isLoading: datasourceLoading } = useDatasourceList('kafka')

  const [triggerType, setTriggerType] = useState<string>('SCHD')
  const [name, setName] = useState('')
  const [cronExpression, setCronExpression] = useState('0 10 * * *')
  const [kafkaExpression, setKafkaExpression] = useState('')
  const [kafkaExpressionType, setKafkaExpressionType] = useState('Groovy')
  const [datasourceId, setDatasourceId] = useState('')
  const [inputs, setInputs] = useState('{}')
  const [outputs, setOutputs] = useState('')

  const { isCurrentWorkspaceEditor } = useAppContext()

  const getExpression = () => {
    if (triggerType === 'KFKA')
      return kafkaExpression

    return cronExpression
  }

  const getExpressionType = () => {
    if (triggerType === 'KFKA')
      return kafkaExpressionType

    return ''
  }

  const isCreatingRef = useRef(false)
  const [isDebuging, setIsDebuging] = useState(false)
  const onDebug = async () => {
    try {
      setIsDebuging(true)
      const res = await debugPublishedWorkflow({ workflowId, inputs, responseMode: 'blocking', triggerFrom: 'DEBUG' })

      setOutputs(JSON.stringify(res, null, 2))
      if (res.data.error === null)
        notify({ type: 'success', message: t('workflow.trigger.debug.success') })
      else
        notify({ type: 'error', message: t('workflow.trigger.debug.fail') })
    }
    catch (e) {
      notify({ type: 'error', message: `${t('workflow.trigger.debug.fail')}` })
    }
    finally {
      setIsDebuging(false)
    }
  }
  const onCreate: MouseEventHandler = useCallback(async () => {
    if (!triggerType) {
      notify({ type: 'error', message: t('app.newApp.appTypeRequired') })
      return
    }
    if (!name.trim()) {
      notify({ type: 'error', message: t('app.newApp.nameNotEmpty') })
      return
    }

    if (triggerType === 'SCHD' && !cronExpression) {
      notify({ type: 'error', message: t('workflow.trigger.crontabRequired') })
      return
    }

    if (triggerType === 'KFKA' && !datasourceId) {
      notify({ type: 'error', message: t('workflow.trigger.datasourceRequired') })
      return
    }

    if (isCreatingRef.current)
      return
    isCreatingRef.current = true
    try {
      const trigger = await createSchedulingTrigger({
        name,
        expression: getExpression(),
        expressionType: getExpressionType(),
        inputs,
        workflowId,
        triggerType,
        datasourceId,
      })
      notify({ type: 'success', message: t('app.newApp.appCreated') })
      onSuccess()
      onClose()
    }
    catch (e) {
      notify({ type: 'error', message: t('app.newApp.appCreateFailed') })
    }
    isCreatingRef.current = false
  }, [name, notify, t, triggerType, cronExpression, onSuccess, onClose, mutateApps, push, isCurrentWorkspaceEditor, datasourceId, datasourceList, kafkaExpression, inputs, outputs])
  return (
    <Modal
      overflowVisible
      className='!p-0 !max-w-[720px] !w-[720px] rounded-xl'
      isShow={show}
      onClose={() => { }}
    >
      {/* Heading */}
      <div className='shrink-0 flex flex-col h-full bg-white rounded-t-xl'>
        <div className='shrink-0 pl-8 pr-6 pt-6 pb-3 bg-white text-xl rounded-t-xl leading-[30px] font-semibold text-gray-900 z-10'>{t('workflow.trigger.startFromBlank')}</div>
      </div>
      {/* trigger type */}
      <div className='py-2 px-8'>
        <div className='py-2 text-sm leading-[20px] font-medium text-gray-900'>{t('workflow.trigger.captionTriggerType')}</div>
        <div className='flex'>
          <TooltipPlus
            hideArrow
            popupContent={
              <div className='max-w-[280px] leading-[18px] text-xs text-gray-700'>{t('workflow.trigger.schdDescription')}</div>
            }
          >
            <div
              className={cn(
                'relative grow box-border w-[158px] mr-2 px-0.5 pt-3 pb-2 flex flex-col items-center justify-center gap-1 rounded-lg border border-gray-100 bg-white text-gray-700 cursor-pointer shadow-xs hover:border-gray-300',
                s['grid-bg-chat'],
                triggerType === 'SCHD' && 'border-[1.5px] border-primary-400 hover:border-[1.5px] hover:border-primary-400',
              )}
              onClick={() => {
                setTriggerType('SCHD')
              }}
            >
              <div className='h-5 text-[13px] font-medium leading-[18px]'>{t('workflow.trigger.types.schd')}</div>
            </div>
          </TooltipPlus>
          <TooltipPlus
            hideArrow
            popupContent={
              <div className='flex flex-col max-w-[320px] leading-[18px] text-xs'>
                <div className='text-gray-700'>{t('workflow.trigger.kafkaDescription')}</div>
              </div>
            }
          >
            <div
              className={cn(
                'relative grow box-border w-[158px] px-0.5 pt-3 pb-2 flex flex-col items-center justify-center gap-1 rounded-lg border border-gray-100 text-gray-700 cursor-pointer bg-white shadow-xs hover:border-gray-300',
                s['grid-bg-workflow'],
                triggerType === 'KFKA' && 'border-[1.5px] border-primary-400 hover:border-[1.5px] hover:border-primary-400',
              )}
              onClick={() => {
                setTriggerType('KFKA')
              }}
            >
              <div className='h-5 text-[13px] font-medium leading-[18px]'>{t('workflow.trigger.types.kafka')}</div>
            </div>
          </TooltipPlus>
        </div>
      </div>

      {/* name */}
      <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.name')}</div>
        <div className='flex items-center justify-between space-x-2'>
          <input
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder={t('workflow.trigger.namePlaceholder') || ''}
            className='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
          />
        </div>
      </div>
      {/* description */}
      {/* <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.description')}</div>
        <textarea
          className='w-full px-3 py-2 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs h-[80px] resize-none'
          placeholder={t('workflow.trigger.descriptionPlaceholder') || ''}
          value={description}
          onChange={e => setDescription(e.target.value)}
        />
      </div> */}

      {/*  datasource */}
      {triggerType === 'KFKA' && (
        <div className='pt-2 px-8'>
          <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.datasource')}</div>
          <div className='flex items-center justify-between space-x-2'>
            <SimpleSelect
              wrapperClassName='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
              onSelect={(item) => { setDatasourceId(item.value.toString()) }}
              items={datasourceList?.map(d => ({ value: d.datasourceId, name: d.name })) || []}
              placeholder='请选择数据源'
            />
          </div>
        </div>
      )}

      {triggerType === 'KFKA' && (
        <div className='pt-2 px-8'>
          <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.expressionTypeDesc')}</div>
          <div className='flex items-center justify-between space-x-2'>
            <SimpleSelect
              wrapperClassName='grow h-10 px-3 text-sm font-normal bg-gray-100 rounded-lg border border-transparent outline-none appearance-none caret-primary-600 placeholder:text-gray-400 hover:bg-gray-50 hover:border hover:border-gray-300 focus:bg-gray-50 focus:border focus:border-gray-300 focus:shadow-xs'
              onSelect={(item) => { setKafkaExpressionType(item.value.toString()) }}
              items={[{ value: 'Aviator', name: 'Aviator' }, { value: 'Groovy', name: 'Groovy' }]}
              placeholder='请选择脚本语言类型'
              defaultValue='Groovy'
            />
          </div>
        </div>
      )}

      {triggerType === 'KFKA' && (
        <div className='pt-2 px-8'>
          <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.expression')}</div>
          <Editor
            width="800"
            height="120px"
            language={kafkaExpressionType === 'Groovy' ? 'groovy' : 'javascript'}
            theme="vs-dark"
            value={kafkaExpression}
            options={{
              lineNumbers: 'on',
              tabSize: 2,
            }}
            onChange={(newValue, e) => setKafkaExpression(newValue || '')}
          />
        </div>
      )}

      {/*  crontab expression */}
      { triggerType === 'SCHD' && (
        <div className='pt-2 px-8'>
          <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.crontab')}</div>
          <Cron clearButton={false} value={cronExpression} setValue={setCronExpression} clockFormat={'24-hour-clock'} allowEmpty={ 'never' } locale={DEFAULT_LOCALE_CHS}/>
          {/* <span>{cronExpression}</span> */}
        </div>
      )}
      <div className='pt-2 px-8'>
        <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.inputs')}</div>
        <Editor
          width="800"
          height="100px"
          language="json"
          theme="vs-dark"
          value={inputs}
          options={{
            lineNumbers: 'on',
            tabSize: 2,
          }}
          onChange={(newValue, e) => setInputs(newValue || '{}')}
        />
      </div>

      { triggerType === 'SCHD' && (
        <div className='pt-2 px-8'>
          <div className='py-2 text-sm font-medium leading-[20px] text-gray-900'>{t('workflow.trigger.outputs')}</div>
          <Editor
            width="800"
            height="100px"
            language="json"
            theme="vs-dark"
            value={outputs}
            options={{
              lineNumbers: 'on',
              tabSize: 2,
              readOnly: true,
            }}
          />
        </div>
      )}

      <div className='px-8 py-6 flex justify-between'>
        { triggerType === 'SCHD' && (<Button variant="primary" onClick={onDebug} disabled={isDebuging}>
          {isDebuging ? t('workflow.trigger.debug.onRun') : t('workflow.trigger.debug.onIdle')}</Button>
        )}
        { triggerType !== 'SCHD' && (<div></div>)}
        <div className='flex'>
          <Button className='mr-2' onClick={onClose}>{t('app.newApp.Cancel')}</Button>
          <Button variant="primary" onClick={onCreate}>{t('app.newApp.Create')}</Button>
        </div>
      </div>
      <div className='absolute right-6 top-6 p-2 cursor-pointer z-20' onClick={onClose}>
        <RiCloseLine className='w-4 h-4 text-gray-500' />
      </div>
    </Modal>
  )
}

export default CreateTriggerModal
