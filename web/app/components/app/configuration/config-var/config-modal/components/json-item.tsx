import React, { useCallback, useEffect, useState } from 'react'
import cn from 'classnames'
import { useTranslation } from 'react-i18next'
import produce from 'immer'
import s from '../style.module.css'
import Selector from './json-item-selector'
import Switch from '@/app/components/base/switch'
import type { InputVar } from '@/app/components/workflow/types'
import { VarType } from '@/app/components/workflow/types'
import { Add, Delete } from '@/app/components/base/icons/src/vender/workflow'

const MethodOptions = [
  { value: 'string', label: 'String' },
  { value: 'number', label: 'Number' },
  { value: 'boolean', label: 'Boolean' },
  { value: 'object', label: 'Object' },
]

type Props = {
  index: number
  value: InputVar
  onChange: (index: number, value: any) => void
}
const inputClassName = ' px-3 text-sm leading-9 text-gray-900 border-0 rounded-lg h-9 bg-gray-100 focus:outline-none focus:ring-1 focus:ring-inset focus:ring-gray-200'

const Item: React.FC<Props> = ({
  index,
  value,
  onChange,
}) => {
  const { t } = useTranslation()
  const isChild = value.varType === VarType.object || value.varType === VarType.arrayObject
  const [item, setItem] = useState<InputVar>(value)

  useEffect(() => {
    setItem(value)
  }, [value])

  const handleSelfItemChange = useCallback((key: string) => {
    return (v: any) => {
      const newItem = produce(item, (draft: InputVar) => {
        if (key === 'varType' && v !== 'object')
          draft.children = []

        draft[key] = v
      })
      setItem(newItem)
      onChange(index, newItem)
    }
  }, [t, onChange])

  const handleChildrenItemChange = useCallback((i: number, value: InputVar | undefined) => {
    const newItem = produce(item, (draft: InputVar) => {
      if (value)
        draft.children[i] = value

      else
        draft.children?.splice(i, 1)
    })
    setItem(newItem)
    onChange(index, newItem)
  }, [onChange])

  const createItem = useCallback((e) => {
    const newItem = produce(item, (draft: InputVar) => {
      if (!draft.children)
        draft.children = []
      draft.children?.push({
        variable: '',
        varType: 'string',
        label: '',
      } as InputVar)
    })
    setItem(newItem)
    onChange(index, newItem)
  }, [t, onChange])

  const deleteItem = useCallback((e) => {
    onChange(index, undefined)
  }, [onChange])

  return (
    <div className={cn('mt-2', s.flex21Auto)}>
      <div className='flex items-center space-x-1'>
        <input
          type='text'
          className={cn(inputClassName, s.width100)}
          value={item.variable}
          onChange={e => handleSelfItemChange('variable')(e.target.value)}
          placeholder={t('appDebug.variableConig.inputPlaceholder')!}
        />
        <div>
          <Selector
            popupClassName='top-[34px] hh'
            itemClassName='capitalize'
            trigger={
              <div
                onClick={(e) => {
                  if (!isChild)
                    e.stopPropagation()
                }}
                className={cn(
                  isChild
                    ? 'cursor-pointer w-[74px]'
                    : 'pointer-events-none w-[74px]',
                  'shrink-0 flex items-center h-8 justify-between px-2.5 rounded-lg bg-gray-100 capitalize',
                )}
              >
                <div className='text-[13px] font-normal text-gray-900'>
                  {item?.varType}
                </div>
              </div>
            }
            value={item?.varType}
            onChange={e => handleSelfItemChange('varType')(e)}
            options={MethodOptions}
          />
        </div>
        <div>
          <input
            type='text'
            className={inputClassName}
            value={item.label as string}
            onChange={e => handleSelfItemChange('label')(e.target.value)}
            placeholder={t('请输入描述')!}
          />
        </div>
        <div>
          <div className='m-[4px] z-11'>
            <Switch
              size={'md'}
              defaultValue={item?.required}
              onChange={e => handleSelfItemChange('required')(e)}
            />
          </div>
        </div>
        <div className='inline-block flex'>

          <div className={cn('w-[24px] h-[24px]', item?.isRoot ? s.marginLeft30 : '')}>
            {item?.varType === 'object' && (
              <div className='w-[24px] h-[24px] cursor-pointer' onClick={createItem}>
                <Add className='w-[24px] h-[24px]'/>
              </div>
            )}
          </div>

          {
            !item?.isRoot && (<div
              className='cursor-pointer'
              onClick={deleteItem}
            >
              <Delete className='w-[24px] h-[24px]'/>
            </div>)
          }
        </div>
      </div>
      {value.children?.map(
        (child, index) => (

          <div key={index} className='ml-4'>
            <Item
              index={index}
              value={child}
              onChange={handleChildrenItemChange}
            />
          </div>
        ),
      )}
    </div>
  )
}
export default React.memo(Item)
