import {
  memo,
  useMemo,
} from 'react'
import { useTranslation } from 'react-i18next'
import { useNodes } from 'reactflow'
import type { ComparisonOperator } from '../types'
import {
  comparisonOperatorNotRequireValue,
  isComparisonOperatorNeedTranslate,
} from '../utils'
import { Variable02 } from '@/app/components/base/icons/src/vender/solid/development'
import { Env } from '@/app/components/base/icons/src/vender/line/others'
import cn from '@/utils/classnames'
import { isENV, isSystemVar } from '@/app/components/workflow/nodes/_base/components/variable/utils'
import { isExceptionVariable } from '@/app/components/workflow/utils'
import type {
  CommonNodeType,
  Node,
} from '@/app/components/workflow/types'

type ConditionValueProps = {
  variableSelector: string[]
  operator: ComparisonOperator
  value: string
}
const ConditionValue = ({
  variableSelector,
  operator,
  value,
}: ConditionValueProps) => {
  const { t } = useTranslation()
  const nodes = useNodes()
  const variableName = isSystemVar(variableSelector) ? variableSelector.slice(0).join('.') : variableSelector.slice(1).join('.')
  const operatorName = isComparisonOperatorNeedTranslate(operator) ? t(`workflow.nodes.ifElse.comparisonOperator.${operator}`) : operator
  const notHasValue = comparisonOperatorNotRequireValue(operator)
  const node: Node<CommonNodeType> | undefined = nodes.find(n => n.id === variableSelector[0]) as Node<CommonNodeType>
  const isException = isExceptionVariable(variableName, node?.data.type)
  const formatValue = useMemo(() => {
    if (notHasValue)
      return ''

    return value.replace(/{{#([^#]*)#}}/g, (a, b) => {
      const arr: string[] = b.split('.')
      if (isSystemVar(arr))
        return `{{${b}}}`

      return `{{${arr.slice(1).join('.')}}}`
    })
  }, [notHasValue, value])

  return (
    <div className='flex items-center px-1 h-6 rounded-md bg-workflow-block-parma-bg'>
      {!isENV(variableSelector) && <Variable02 className={cn('shrink-0 mr-1 w-3.5 h-3.5 text-text-accent', isException && 'text-text-warning')} />}
      {isENV(variableSelector) && <Env className='shrink-0 mr-1 w-3.5 h-3.5 text-util-colors-violet-violet-600' />}
      <div
        className={cn(
          'shrink-0  truncate text-xs font-medium text-text-accent',
          !notHasValue && 'max-w-[70px]',
          isException && 'text-text-warning',
        )}
        title={variableName}
      >
        {variableName}
      </div>
      <div
        className='shrink-0 mx-1 text-xs font-medium text-text-primary'
        title={operatorName}
      >
        {operatorName}
      </div>
      {
        !notHasValue && (
          <div className='truncate text-xs text-text-secondary' title={formatValue}>{formatValue}</div>
        )
      }
    </div>
  )
}

export default memo(ConditionValue)
