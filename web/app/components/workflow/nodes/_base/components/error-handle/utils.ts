import type { CommonNodeType } from '@/app/components/workflow/types'
import {
  BlockEnum,
  ResponseType,
  VarType,
} from '@/app/components/workflow/types'
import type { CodeNodeType } from '@/app/components/workflow/nodes/code/types'
import type { HttpNodeType } from '@/app/components/workflow/nodes/http/types'

const getDefaultValueByType = (type: VarType) => {
  if (type === VarType.string)
    return ''

  if (type === VarType.number)
    return 0

  if (type === VarType.object)
    return '{}'

  if (type === VarType.arrayObject || type === VarType.arrayString || type === VarType.arrayNumber || type === VarType.arrayFile)
    return '[]'

  return ''
}

/**
 * 为错误处理策略生成默认值
 * @param nodeInputs 节点输入数据
 * @returns 默认值数组
 */
export const getDefaultValueForErrorStrategy = (nodeInputs: CommonNodeType) => {
  if (nodeInputs.type === BlockEnum.HttpRequest) {
    const httpInputs = nodeInputs as HttpNodeType
    if (httpInputs.response?.type === ResponseType.string) {
      return [
        {
          key: 'body',
          type: VarType.string,
          value: getDefaultValueByType(VarType.string),
        },
        {
          key: 'status_code',
          type: VarType.number,
          value: getDefaultValueByType(VarType.number),
        },
        {
          key: 'headers',
          type: VarType.object,
          value: getDefaultValueByType(VarType.object),
        },
      ]
    }
    else {
      return [
        {
          key: 'body',
          type: VarType.object,
          value: getDefaultValueByType(VarType.object),
        },
        {
          key: 'status_code',
          type: VarType.number,
          value: getDefaultValueByType(VarType.number),
        },
        {
          key: 'headers',
          type: VarType.object,
          value: getDefaultValueByType(VarType.object),
        },
      ]
    }
  }
  else if (nodeInputs.type === BlockEnum.LLM) {
    return [
      {
        key: 'text',
        type: VarType.string,
        value: getDefaultValueByType(VarType.string),
      },
      {
        key: 'reasoning_content',
        type: VarType.string,
        value: getDefaultValueByType(VarType.string),
      },
    ]
  }

  return []
}

export const getDefaultValue = (data: CommonNodeType) => {
  const { type } = data

  if (type === BlockEnum.LLM) {
    return [{
      key: 'text',
      type: VarType.string,
      value: getDefaultValueByType(VarType.string),
    }]
  }

  if (type === BlockEnum.HttpRequest) {
    return [
      {
        key: 'body',
        type: VarType.string,
        value: getDefaultValueByType(VarType.string),
      },
      {
        key: 'status_code',
        type: VarType.number,
        value: getDefaultValueByType(VarType.number),
      },
      {
        key: 'headers',
        type: VarType.object,
        value: getDefaultValueByType(VarType.object),
      },
    ]
  }

  if (type === BlockEnum.Tool) {
    return [
      {
        key: 'text',
        type: VarType.string,
        value: getDefaultValueByType(VarType.string),
      },
      {
        key: 'json',
        type: VarType.arrayObject,
        value: getDefaultValueByType(VarType.arrayObject),
      },
    ]
  }

  if (type === BlockEnum.Code) {
    const { outputs } = data as CodeNodeType

    return Object.keys(outputs).map((key) => {
      return {
        key,
        type: outputs[key].type,
        value: getDefaultValueByType(outputs[key].type),
      }
    })
  }

  return []
}
