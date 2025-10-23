'use client'
import type { FC } from 'react'
import React, { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useBoolean } from 'ahooks'
import type { Param } from '../../types'
import ListNoDataPlaceholder from '../../../_base/components/list-no-data-placeholder'
import Item from './item'
import EditParam from './update'
import type { MoreInfo } from '@/app/components/workflow/types'

const i18nPrefix = 'workflow.nodes.parameterExtractor'

type Props = {
  readonly: boolean
  list: Param[]
  onChange: (list: Param[], moreInfo?: MoreInfo) => void
}

const List: FC<Props> = ({
  readonly,
  list,
  onChange,
}) => {
  const { t } = useTranslation()
  const [isShowEditModal, {
    setTrue: showEditModal,
    setFalse: hideEditModal,
  }] = useBoolean(false)

  const handleItemChange = useCallback((index: number) => {
    return (payload: Param, moreInfo?: MoreInfo) => {
      const newList = list.map((item, i) => {
        if (i === index)
          return payload

        return item
      })
      onChange(newList, moreInfo)
      hideEditModal()
    }
  }, [hideEditModal, list, onChange])

  const [currEditItemIndex, setCurrEditItemIndex] = useState<number>(-1)

  const handleItemEdit = useCallback((index: number) => {
    return () => {
      if (readonly)
        return
      setCurrEditItemIndex(index)
      showEditModal()
    }
  }, [showEditModal, readonly])

  const handleItemDelete = useCallback((index: number) => {
    return () => {
      if (readonly)
        return
      const newList = list.filter((_, i) => i !== index)
      onChange(newList)
    }
  }, [list, onChange, readonly])

  if (list.length === 0) {
    return (
      <ListNoDataPlaceholder >{t(`${i18nPrefix}.extractParametersNotSet`)}</ListNoDataPlaceholder>
    )
  }
  return (
    <div className='space-y-1'>
      {list.map((item, index) => (
        <Item
          key={index}
          readonly={readonly}
          payload={item}
          onDelete={handleItemDelete(index)}
          onEdit={handleItemEdit(index)}
        />
      ))}
      {isShowEditModal && (
        <EditParam
          type='edit'
          payload={list[currEditItemIndex]}
          onSave={handleItemChange(currEditItemIndex)}
          onCancel={hideEditModal}
        />
      )}
    </div>
  )
}
export default React.memo(List)
