import { useCallback, useEffect, useState } from 'react'
import produce from 'immer'
import { useBoolean } from 'ahooks'
import useVarList from '../_base/hooks/use-var-list'
import type { ValueSelector, Var } from '../../types'
import { ResponseType, VarType } from '../../types'
import { useStore } from '../../store'
import type { Authorization, Body, HttpNodeType, Method, ResponseBody, Timeout } from './types'
import useKeyValueList from './hooks/use-key-value-list'
import { convertJsonToVariables } from '@/app/components/workflow/utils'
import nodeDefault from '@/app/components/workflow/nodes/http/default'
import useNodeCrud from '@/app/components/workflow/nodes/_base/hooks/use-node-crud'
import useOneStepRun from '@/app/components/workflow/nodes/_base/hooks/use-one-step-run'
import {
  useIsChatMode,
  useNodesReadOnly, useWorkflow,
} from '@/app/components/workflow/hooks'

const useConfig = (id: string, payload: HttpNodeType) => {
  const { nodesReadOnly: readOnly } = useNodesReadOnly()

  const isChatMode = useIsChatMode()
  const defaultConfig = useStore(s => s.nodesDefaultConfigs)[payload.type]

  const { inputs, setInputs } = useNodeCrud<HttpNodeType>(id, payload)
  const [isShowRemoveVarConfirm, { setTrue: showRemoveVarConfirm, setFalse: hideRemoveVarConfirm }] = useBoolean(false)
  const { handleOutVarRenameChange, isVarUsedInNodes, removeUsedVarInNodes } = useWorkflow()
  const [removedVar, setRemovedVar] = useState<ValueSelector[]>([])
  const [key, setKey] = useState<number>(1)
  const [newResponse, setNewResponse] = useState<ResponseBody>()
  const defaultConfigDefault = nodeDefault.defaultValue
  const { handleVarListChange, handleAddVariable } = useVarList<HttpNodeType>({
    inputs,
    setInputs,
  })

  useEffect(() => {
    const isReady = defaultConfig && Object.keys(defaultConfig).length > 0
    if (isReady) {
      setInputs({
        ...defaultConfig,
        ...defaultConfigDefault,
        ...inputs,
      })
    }
    else {
      setInputs({
        ...defaultConfigDefault,
        ...inputs,
      })
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [defaultConfig, defaultConfigDefault])

  const handleMethodChange = useCallback((method: Method) => {
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.method = method
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  const handleUrlChange = useCallback((url: string) => {
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.url = url
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  const handleCallbackChange = useCallback((waitCallback: boolean) => {
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.waitCallback = waitCallback
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  const handleFieldChange = useCallback((field: string) => {
    return (value: string) => {
      const newInputs = produce(inputs, (draft: HttpNodeType) => {
        (draft as any)[field] = value
      })
      setInputs(newInputs)
    }
  }, [inputs, setInputs])

  const {
    list: headers,
    setList: setHeaders,
    addItem: addHeader,
    isKeyValueEdit: isHeaderKeyValueEdit,
    toggleIsKeyValueEdit: toggleIsHeaderKeyValueEdit,
  } = useKeyValueList(inputs.headers, handleFieldChange('headers'))

  const {
    list: params,
    setList: setParams,
    addItem: addParam,
    isKeyValueEdit: isParamKeyValueEdit,
    toggleIsKeyValueEdit: toggleIsParamKeyValueEdit,
  } = useKeyValueList(inputs.params, handleFieldChange('params'))

  const setBody = useCallback((data: Body) => {
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.body = data
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  // authorization
  const [isShowAuthorization, {
    setTrue: showAuthorization,
    setFalse: hideAuthorization,
  }] = useBoolean(false)

  const setAuthorization = useCallback((authorization: Authorization) => {
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.authorization = authorization
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  const setTimeout = useCallback((timeout: Timeout) => {
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.timeout = timeout
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  const filterVar = useCallback((varPayload: Var) => {
    return [VarType.string, VarType.number, VarType.secret].includes(varPayload.type)
  }, [])

  // single run
  const {
    isShowSingleRun,
    hideSingleRun,
    getInputVars,
    runningStatus,
    handleRun,
    handleStop,
    runInputData,
    setRunInputData,
    runResult,
  } = useOneStepRun<HttpNodeType>({
    id,
    data: inputs,
    defaultRunInputData: {},
  })

  const varInputs = getInputVars([
    inputs.url,
    inputs.headers,
    inputs.params,
    inputs.body.data,
  ])

  const inputVarValues = (() => {
    const vars: Record<string, any> = {}
    Object.keys(runInputData)
      .forEach((key) => {
        vars[key] = runInputData[key]
      })
    return vars
  })()

  const setInputVarValues = useCallback((newPayload: Record<string, any>) => {
    setRunInputData(newPayload)
  }, [setRunInputData])
  const convert = function (body: ResponseBody) {
    return {
      type: body.type === ResponseType.json ? VarType.object : VarType.string,
      variable: 'body',
      ...(body.type === ResponseType.json && { children: convertJsonToVariables(body.data) }),
    }
  }
  const varSelectorConvert = function (path: string[], vars: Var[]): string[[]] {
    const varResult: string[][] = []
    vars.forEach((v) => {
      const paths = [...path, v.variable]
      varResult.push(paths)
      if (v.children && v.children.length > 0)
        varResult.push(...varSelectorConvert(paths, v.children))
    })
    return varResult
  }

  const handleResponseBody = useCallback((body: ResponseBody) => {
    if (body.type === inputs.response.type && body.type === ResponseType.json && !convertJsonToVariables(body.data))
      return

    setNewResponse(body)
    const newOutput = convert(body)
    const newVars = varSelectorConvert([id], [newOutput])
    const oldVars = varSelectorConvert([id], [inputs.output])
    const newVarSelectors = newVars.map((v: any[]) => v.join('.'))
    const deleteVarSelectorList: any[] = []
    oldVars.forEach((v: any[]) => {
      if (!newVarSelectors.includes(v.join('.')))
        deleteVarSelectorList.push(v)
    })

    const removeVarSelectorList: any[] | ((prevState: ValueSelector[]) => ValueSelector[]) = []
    deleteVarSelectorList.forEach((v) => {
      if (isVarUsedInNodes(v))
        removeVarSelectorList.push(v)
    })
    if (removeVarSelectorList.length > 0) {
      setRemovedVar(removeVarSelectorList)
      showRemoveVarConfirm()
      return
    }

    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.response = body
      draft.output = convert(body)
    })
    setInputs(newInputs)
  }, [inputs, setInputs])

  const removeVarInNode = useCallback(() => {
    removedVar.map(removeUsedVarInNodes)
    hideRemoveVarConfirm()
    const newInputs = produce(inputs, (draft: HttpNodeType) => {
      draft.response = newResponse
      draft.output = convert(newResponse)
    })
    setInputs(newInputs)
  }, [hideRemoveVarConfirm, removeUsedVarInNodes, removedVar])

  const handleRemoveVarConfirm = useCallback(() => {
    hideRemoveVarConfirm()
    setKey(key + 1)
  }, [hideRemoveVarConfirm, inputs, setInputs, key, setKey])

  const convertVarToVarItemProps = (item: Var): any => {
    if (!item)
      return undefined
    const { variable, type, children } = item
    return {
      name: variable,
      type,
      description: '',
      subItems: children ? children.map(convertVarToVarItemProps) : undefined,
    }
  }
  const outputVar = convertVarToVarItemProps(inputs.output)

  return {
    isChatMode,
    readOnly,
    inputs,
    handleVarListChange,
    handleAddVariable,
    filterVar,
    handleMethodChange,
    handleUrlChange,
    // headers
    headers,
    setHeaders,
    addHeader,
    isHeaderKeyValueEdit,
    toggleIsHeaderKeyValueEdit,
    // params
    params,
    setParams,
    addParam,
    isParamKeyValueEdit,
    toggleIsParamKeyValueEdit,
    // body
    setBody,
    // authorization
    isShowAuthorization,
    showAuthorization,
    hideAuthorization,
    setAuthorization,
    setTimeout,
    // single run
    isShowSingleRun,
    hideSingleRun,
    runningStatus,
    handleRun,
    handleStop,
    varInputs,
    inputVarValues,
    setInputVarValues,
    runResult,
    outputVar,
    handleResponseBody,
    isShowRemoveVarConfirm,
    handleRemoveVarConfirm,
    removeVarInNode,
    key,
    handleCallbackChange,
  }
}

export default useConfig
