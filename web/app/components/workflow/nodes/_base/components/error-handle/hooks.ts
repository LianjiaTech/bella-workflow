import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { ErrorHandleTypeEnum } from './types'
import type { DefaultValueForm } from './types'
import { getDefaultValueForErrorStrategy } from './utils'
import type { CommonNodeType } from '@/app/components/workflow/types'
import { BlockEnum } from '@/app/components/workflow/types'
import type { HttpNodeType } from '@/app/components/workflow/nodes/http/types'

import {
  useEdgesInteractions,
  useNodeDataUpdate,
} from '@/app/components/workflow/hooks'
import useNodeCrud from '@/app/components/workflow/nodes/_base/hooks/use-node-crud'
import defaultValue from '@/app/components/workflow/nodes/_base/components/error-handle/default-value'

export const useDefaultValue = (
  id: string,
) => {
  const { handleNodeDataUpdateWithSyncDraft } = useNodeDataUpdate()
  const handleFormChange = useCallback((
    {
      key,
      value,
      type,
    }: DefaultValueForm,
    data: CommonNodeType,
  ) => {
    const default_value = data.default_value || []
    const index = default_value.findIndex(form => form.key === key)

    if (index > -1) {
      const newDefaultValue = [...default_value]
      newDefaultValue[index].value = value
      handleNodeDataUpdateWithSyncDraft({
        id,
        data: {
          default_value: newDefaultValue,
        },
      })
      return
    }

    handleNodeDataUpdateWithSyncDraft({
      id,
      data: {
        default_value: [
          ...default_value,
          {
            key,
            value,
            type,
          },
        ],
      },
    })
  }, [handleNodeDataUpdateWithSyncDraft, id])

  return {
    handleFormChange,
  }
}

export const useErrorHandle = (
  id: string,
  data: CommonNodeType,
) => {
  const initCollapsed = useMemo(() => {
    if (data.error_strategy === ErrorHandleTypeEnum.none)
      return true

    return false
  }, [data.error_strategy])
  const [collapsed, setCollapsed] = useState(initCollapsed)
  const { handleNodeDataUpdateWithSyncDraft } = useNodeDataUpdate()
  const { handleEdgeDeleteByDeleteBranch } = useEdgesInteractions()
  const { inputs: nodeInputs } = useNodeCrud(id, data)

  // 提取HTTP节点的response类型和output类型，用于依赖数组
  const httpTypeInfo = useMemo(() => {
    if (nodeInputs.type === BlockEnum.HttpRequest) {
      const httpNode = nodeInputs as HttpNodeType
      return {
        responseType: httpNode.response?.type,
      }
    }
    return undefined
  }, [nodeInputs])

  // 当 nodeInputs 变化时，更新值为 null 的字段
  useEffect(() => {
    // 只有当错误处理策略为 defaultValue 时才执行
    if (data.error_strategy === ErrorHandleTypeEnum.defaultValue) {
      const newDefaultValues = getDefaultValueForErrorStrategy(nodeInputs)
      const currentDefaultValue = data.default_value || []

      // 检查是否需要更新
      // 比较newDefaultValues和currentDefaultValue是否有实质性差异
      let needUpdate = false

      // 如果长度不同，肯定需要更新
      if (currentDefaultValue.length !== newDefaultValues.length) {
        needUpdate = true
      }
      else {
        // 检查是否有新增的key
        const currentKeys = new Set(currentDefaultValue.map(item => item.key))
        const hasNewKey = newDefaultValues.some(item => !currentKeys.has(item.key))
        if (hasNewKey) {
          needUpdate = true
        }
        else {
          // 检查类型是否有变化
          const hasTypeChange = newDefaultValues.some((newItem) => {
            const existingItem = currentDefaultValue.find(item => item.key === newItem.key)
            return existingItem && existingItem.type !== newItem.type
          })
          if (hasTypeChange)
            needUpdate = true
        }
      }

      // 只有在需要更新时才执行更新操作
      if (needUpdate) {
        // 1. 如果currentDefaultValue是空的，则以newDefaultValues为主
        if (currentDefaultValue.length === 0) {
          handleNodeDataUpdateWithSyncDraft({
            id,
            data: {
              default_value: newDefaultValues,
            },
          })
          return
        }

        // 2. 对newDefaultValues和currentDefaultValue做diff
        // 2.1. 最终default_value中有哪些字段，都以以newDefaultValues为主(每个default_value中的实体，key是主键)，但是如果这个字段之前在currentDefaultValue中存在，则保留原来的值
        const mergedDefaultValues = newDefaultValues.map((newItem) => {
          // 查找当前default_value中是否已存在相同key的项
          const existingItem = currentDefaultValue.find(item => item.key === newItem.key)

          // 如果存在，保留原来的值，但使用新的类型
          if (existingItem) {
            return {
              key: newItem.key,
              type: newItem.type,
              value: existingItem.value,
            }
          }

          // 如果不存在，使用新的默认值
          return newItem
        })

        // 更新节点数据
        handleNodeDataUpdateWithSyncDraft({
          id,
          data: {
            default_value: mergedDefaultValues,
          },
        })
      }
    }
  }, [
    nodeInputs,
    httpTypeInfo,
    id,
    handleNodeDataUpdateWithSyncDraft,
    data.error_strategy,
  ])

  const handleErrorHandleTypeChange = useCallback((value: ErrorHandleTypeEnum, data: CommonNodeType) => {
    if (data.error_strategy === value)
      return

    if (value === ErrorHandleTypeEnum.none) {
      handleNodeDataUpdateWithSyncDraft({
        id,
        data: {
          error_strategy: undefined,
          default_value: undefined,
        },
      })
      setCollapsed(true)
      handleEdgeDeleteByDeleteBranch(id, ErrorHandleTypeEnum.failBranch)
    }

    if (value === ErrorHandleTypeEnum.failBranch) {
      handleNodeDataUpdateWithSyncDraft({
        id,
        data: {
          error_strategy: value,
          default_value: undefined,
        },
      })
      setCollapsed(false)
    }

    if (value === ErrorHandleTypeEnum.defaultValue) {
      // 使用 utils.ts 中的函数获取默认值
      const defaultValues = getDefaultValueForErrorStrategy(nodeInputs)

      handleNodeDataUpdateWithSyncDraft({
        id,
        data: {
          error_strategy: value,
          default_value: defaultValues.length > 0 ? defaultValues : defaultValue,
        },
      })
      setCollapsed(false)
      handleEdgeDeleteByDeleteBranch(id, ErrorHandleTypeEnum.failBranch)
    }
  }, [id, nodeInputs, handleNodeDataUpdateWithSyncDraft, handleEdgeDeleteByDeleteBranch])

  return {
    collapsed,
    setCollapsed,
    handleErrorHandleTypeChange,
  }
}
