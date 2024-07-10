'use client'
import type { FC } from 'react'
import React, { useCallback, useEffect } from 'react'
import produce from 'immer'
import cn from 'classnames'
import InputWithVar from '@/app/components/workflow/nodes/_base/components/prompt/editor'
import type { Var } from '@/app/components/workflow/types'
import {ResponseType, VarType} from '@/app/components/workflow/types'
import useAvailableVarList from "@/app/components/workflow/nodes/_base/hooks/use-available-var-list";
import useKeyValueList from "@/app/components/workflow/nodes/http/hooks/use-key-value-list";
import {ResponseBody} from "@/app/components/workflow/nodes/http/types";
import {useTranslation} from "react-i18next";

type Props = {
  readonly: boolean
  nodeId: string
  payload: ResponseBody
  placeholder: string
  onChange: (payload: ResponseBody) => void
}

const allTypes = [
  ResponseType.string,
  ResponseType.json,
]
const bodyTextMap = {
  [ResponseType.string]: 'String',
  [ResponseType.json]: 'JSON',
}

const ResponseBody: FC<Props> = ({
  readonly,
  nodeId,
  payload,
  placeholder,
  onChange,
}) => {

  const { t } = useTranslation()

  const { type } = payload
  const { availableVars, availableNodes } = useAvailableVarList(nodeId, {
    onlyLeafNodeVar: false,
    filterVar: (varPayload: Var) => {
      return [VarType.string, VarType.number].includes(varPayload.type)
    },
  })

  const handleTypeChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const newType = e.target.value as ResponseType
    onChange({
      type: newType,
      data: '',
    })
    // eslint-disable-next-line @typescript-eslint/no-use-before-define

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onChange])

  const isCurrentKeyValue = false

  const {
    list: body,
    setList: setBody,
    addItem: addBody,
  } = useKeyValueList(payload.data, (value) => {
    if (!isCurrentKeyValue)
      return

    const newBody = produce(payload, (draft: ResponseBody) => {
      draft.data = value
    })
    onChange(newBody)
  }, type === ResponseType.json)

  useEffect(() => {
    if (!isCurrentKeyValue)
      return

    const newBody = produce(payload, (draft: ResponseBody) => {
      draft.data = body.map((item) => {
        if (!item.key && !item.value)
          return ''
        return `${item.key}:${item.value}`
      }).join('\n')
    })
    onChange(newBody)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isCurrentKeyValue])

  const handleBodyValueChange = useCallback((value: string) => {
    const newBody = produce(payload, (draft: ResponseBody) => {
      draft.data = value
    })
    onChange(newBody)
  }, [onChange, payload])

  return (
    <div>
      {/* body type */}
      <div className='flex flex-wrap'>
        {allTypes.map(t => (
          <label key={t} htmlFor={`response-type-${t}`} className='mr-4 flex items-center h-7 space-x-2'>
            <input
              type="radio"
              id={`response-type-${t}`}
              value={t}
              checked={type === t}
              onChange={handleTypeChange}
              disabled={readonly}
            />
            <div className='leading-[18px] text-[13px] font-normal text-gray-700'>{bodyTextMap[t]}</div>
          </label>
        ))}
      </div>
      {/* body value */}
      <div className={cn(type !== ResponseType.string && 'mt-1')}>
        {type === ResponseType.json && (
          <InputWithVar
            instanceId={'http-response-json'}
            title='JSON'
            value={payload.data}
            onChange={handleBodyValueChange}
            readOnly={readonly}
            placeholder={placeholder}
          />
        )}
      </div>
    </div>
  )
}
export default React.memo(ResponseBody)
