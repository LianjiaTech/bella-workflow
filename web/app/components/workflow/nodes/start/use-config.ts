import { useCallback, useState } from 'react'
import produce from 'immer'
import { useBoolean } from 'ahooks'
import type { StartNodeType } from './types'
import { ChangeType, InputVarType } from '@/app/components/workflow/types'
import type { InputVar, MoreInfo, ValueSelector } from '@/app/components/workflow/types'
import useNodeCrud from '@/app/components/workflow/nodes/_base/hooks/use-node-crud'
import {
  useIsChatMode,
  useNodesReadOnly,
  useWorkflow,
} from '@/app/components/workflow/hooks'

const useConfig = (id: string, payload: StartNodeType) => {
  const { nodesReadOnly: readOnly } = useNodesReadOnly()
  const { handleOutVarRenameChange, isVarUsedInNodes, removeUsedVarInNodes } = useWorkflow()
  const isChatMode = useIsChatMode()

  const { inputs, setInputs } = useNodeCrud<StartNodeType>(id, payload)

  const [isShowAddVarModal, {
    setTrue: showAddVarModal,
    setFalse: hideAddVarModal,
  }] = useBoolean(false)

  const [isShowRemoveVarConfirm, {
    setTrue: showRemoveVarConfirm,
    setFalse: hideRemoveVarConfirm,
  }] = useBoolean(false)
  const [removedVar, setRemovedVar] = useState<ValueSelector[]>([[]])
  const [removedIndex, setRemoveIndex] = useState(-1)
  const [newVarList, setNewVarList] = useState<InputVar[]>()
  const varSelectorConvert = function (path: string[], vars: InputVar[]): string[[]] {
    const varResult = []
    vars.forEach((v) => {
      const paths = [...path, v.variable]
      varResult.push(paths)
      if (v.children && v.children.length > 0)
        varResult.push(...varSelectorConvert(paths, v.children))
    })
    return varResult
  }

  const handleVarListChange = useCallback((newList: InputVar[], moreInfo?: { index: number; payload: MoreInfo }) => {
    if (newList[0]?.type === InputVarType.json) {
      setNewVarList(newList)
      const newVars = varSelectorConvert([id], newList)
      const oldVars = varSelectorConvert([id], inputs.variables)
      const newVarSelectors = newVars.map(v => v.join('.'))
      const deleteVarSelectorList = []
      oldVars.forEach((v) => {
        if (!newVarSelectors.includes(v.join('.')))
          deleteVarSelectorList.push(v)
      })
      const removeVarSelectorList = []
      deleteVarSelectorList.forEach((v) => {
        if (isVarUsedInNodes(v))
          removeVarSelectorList.push(v)
      })
      if (removeVarSelectorList.length > 0) {
        setRemovedVar(removeVarSelectorList)
        showRemoveVarConfirm()
        if (moreInfo?.payload?.type === ChangeType.remove) {
          setRemoveIndex(moreInfo?.index as number)
          return
        }
        return
      }
      else {
        setRemovedVar([])
      }
    }

    if (moreInfo?.payload?.type === ChangeType.remove) {
      if (isVarUsedInNodes([id, moreInfo?.payload?.payload?.beforeKey || ''])) {
        showRemoveVarConfirm()
        setRemovedVar([[id, moreInfo?.payload?.payload?.beforeKey || '']])
        setRemoveIndex(moreInfo?.index as number)
        return
      }
    }

    const newInputs = produce(inputs, (draft: any) => {
      draft.variables = newList
    })
    setInputs(newInputs)
    if (moreInfo?.payload?.type === ChangeType.changeVarName) {
      const changedVar = newList[moreInfo.index]
      handleOutVarRenameChange(id, [id, inputs.variables[moreInfo.index].variable], [id, changedVar.variable])
    }
  }, [handleOutVarRenameChange, id, inputs, isVarUsedInNodes, setInputs, showRemoveVarConfirm])

  const removeVarInNode = useCallback(() => {
    if (removedIndex >= 0) {
      const newInputs = produce(inputs, (draft) => {
        draft.variables.splice(removedIndex, 1)
      })
      setInputs(newInputs)
    }
    removedVar.forEach((v) => {
      removeUsedVarInNodes(v)
    })
    setRemoveIndex(-1)
    hideRemoveVarConfirm()
  }, [hideRemoveVarConfirm, inputs, removeUsedVarInNodes, removedIndex, removedVar, setInputs])

  const handleAddVariable = useCallback((payload: InputVar) => {
    const newInputs = produce(inputs, (draft: StartNodeType) => {
      draft.variables.push(payload)
    })
    setInputs(newInputs)
  }, [inputs, setInputs])
  return {
    readOnly,
    isChatMode,
    inputs,
    isShowAddVarModal,
    showAddVarModal,
    hideAddVarModal,
    handleVarListChange,
    handleAddVariable,
    isShowRemoveVarConfirm,
    hideRemoveVarConfirm,
    onRemoveVarConfirm: removeVarInNode,
  }
}

export default useConfig
