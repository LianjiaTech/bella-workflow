import React, { useCallback, useState } from 'react'
import cn from 'classnames'
import { useTranslation } from 'react-i18next'
import Selector from './selector'
import Switch from '@/app/components/base/switch'
import type { MethodOption } from '@/app/components/workflow/types'
import { Add, Delete } from '@/app/components/base/icons/src/vender/workflow'

type VariableConfig = {
  variable: string
  varType: string
  required: boolean
  description: string
  children?: VariableConfig[]
}

type VariableItemProps = {
  typeOption?: MethodOption
  payload: VariableConfig
  onChange: (payload: VariableConfig) => void
  onDelete: () => void
  child?: boolean
}

const MethodOptions: MethodOption[] = [
  { label: 'string', value: 'string' },
  { label: 'object', value: 'object' },
]

const defaultNewItem: VariableConfig = {
  variable: '',
  varType: 'string',
  required: true,
  description: '',
}

const Item: React.FC<VariableItemProps> = ({ payload, onChange, onDelete, child }) => {
  const { t } = useTranslation()
  const [tempPayload, setTempPayload] = useState<VariableConfig>(payload)

  const handleDataChange = useCallback(
    (value: any, key: keyof VariableConfig) => {
      setTempPayload((prev) => {
        const updatedPayload = { ...prev, [key]: value }
        onChange(updatedPayload)
        return updatedPayload
      })
    },
    [onChange],
  )

  const addNewItem = useCallback(() => {
    const updatedPayload = {
      ...tempPayload,
      children: [...(tempPayload.children || []), { ...defaultNewItem }],
    }
    setTempPayload(updatedPayload)
    onChange(updatedPayload)
  }, [tempPayload, onChange])

  const handleChildChange = useCallback(
    (index: number, newChildPayload: VariableConfig) => {
      setTempPayload((prev) => {
        const updatedChildren = prev.children!.map((child, i) =>
          i === index ? newChildPayload : child,
        )
        const updatedPayload = { ...prev, children: updatedChildren }
        onChange(updatedPayload)
        return updatedPayload
      })
    },
    [onChange],
  )

  const handleChildDelete = useCallback(
    (index: number) => {
      setTempPayload((prev) => {
        const updatedChildren = [
          ...prev.children!.slice(0, index),
          ...prev.children!.slice(index + 1),
        ]
        const updatedPayload = { ...prev, children: updatedChildren }
        setTempPayload(updatedPayload)
        onChange(updatedPayload)
      })
    },
    [onChange],
  )
  return (
    <div className='mt-1'>
      <div className='mt-2 space-y-2'>
        <div className='space-y-2'>
          <div className='flex items-center space-x-1'>
            <div
              className={cn(
                child ? 'w-[146px]' : 'w-[162px]',
                'cursor-pointer',
              )}
            >
              {!child && (
                <div className='leading-8 text-[13px] font-medium text-gray-700'>
                  {t('appDebug.variableConig.varName')}
                </div>
              )}
              <div className='!flex'>
                <div className='border-gray-100 relative flex items-center w-full h-8 p-1 rounded-lg bg-gray-100 border'>
                  <input
                    type='text'
                    className='w-full h-8 leading-8 pl-0.5 bg-transparent text-[13px] font-normal text-gray-900 placeholder:text-gray-400 focus:outline-none'
                    value={tempPayload?.variable}
                    onChange={e =>
                      handleDataChange(e.target.value, 'variable')
                    }
                  />
                </div>
              </div>
            </div>
            <div>
              {!child && (
                <div className='leading-8 text-[13px] font-medium text-gray-700'>
                  {t('appDebug.variableConig.varType')}
                </div>
              )}
              <Selector
                popupClassName='top-[34px] hh'
                itemClassName='capitalize'
                trigger={
                  <div
                    onClick={(e) => {
                      if (!child)
                        e.stopPropagation()
                    }}
                    className={cn(
                      child
                        ? 'cursor-pointer w-[74px]'
                        : 'pointer-events-none w-[100px]',
                      'shrink-0 flex items-center h-8 justify-between px-2.5 rounded-lg bg-gray-100 capitalize',
                    )}
                  >
                    <div className='text-[13px] font-normal text-gray-900'>
                      {tempPayload?.varType}
                    </div>
                  </div>
                }
                value={tempPayload?.varType}
                options={MethodOptions}
                onChange={e => handleDataChange(e, 'varType')}
              />
            </div>
            <div>
              {!child && (
                <div className='leading-8 text-[13px] font-medium text-gray-700'>
                  {t('appDebug.variableConig.describe')}
                </div>
              )}
              <input
                className={cn(
                  child ? 'w-[225px]' : 'w-[241px]',
                  'h-8 leading-8 px-2.5 rounded-lg bg-gray-100 text-gray-900 text-[13px] placeholder:text-gray-400 focus:outline-none',
                )}
                type='text'
                value={tempPayload?.description}
                onChange={e =>
                  handleDataChange(e.target.value, 'description')
                }
              />
            </div>
            <div>
              {!child && (
                <div className='leading-8 text-[13px] font-medium text-gray-700'>
                  {t('appDebug.variableConig.isNecessary')}
                </div>
              )}
              <div className='m-[4px]'>
                <Switch
                  defaultValue={tempPayload?.required}
                  onChange={e => handleDataChange(e, 'required')}
                />
              </div>
            </div>
            {tempPayload?.varType === 'object' && (
              <div>
                {!child && (
                  <div className='leading-8 text-[13px] font-medium text-gray-700'>
                    {t('appDebug.variableConig.operate')}
                  </div>
                )}
                <div className='h-8 mt-1 cursor-pointer' onClick={addNewItem}>
                  <Add className='w-[24px] h-[24px]'/>
                </div>
              </div>
            )}
            {child && (
              <div
                className='h-8 mt-1 cursor-pointer'
                onClick={onDelete}
              >
                <Delete className='w-[24px] h-[24px]'/>
              </div>
            )}
          </div>
          {tempPayload?.children && tempPayload?.children.length > 0 && (
            <div className='ml-4'>
              {tempPayload.children.map((item, index) => (
                <div key={`bella${index}`} className='flex items-center'>
                  <Item
                    payload={item}
                    child={true}
                    onChange={newChildPayload =>
                      handleChildChange(index, newChildPayload)
                    }
                    onDelete={() => handleChildDelete(index)}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
export default React.memo(Item)
