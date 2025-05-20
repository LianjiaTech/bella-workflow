import { useMemo } from 'react'
import { useNodes } from 'reactflow'
import { capitalize } from 'lodash-es'
import { VarBlockIcon } from '@/app/components/workflow/block-icon'
import type {
  CommonNodeType,
  ValueSelector,
  VarType,
} from '@/app/components/workflow/types'
import { BlockEnum } from '@/app/components/workflow/types'
import { Line3 } from '@/app/components/base/icons/src/public/common'
import { Variable02 } from '@/app/components/base/icons/src/vender/solid/development'
import { Env } from '@/app/components/base/icons/src/vender/line/others'
import { isENV, isSystemVar } from '@/app/components/workflow/nodes/_base/components/variable/utils'
import cn from '@/utils/classnames'
import { isExceptionVariable } from '@/app/components/workflow/utils'

type VariableTagProps = {
  valueSelector: ValueSelector
  varType: VarType
}
const VariableTag = ({
  valueSelector,
  varType,
}: VariableTagProps) => {
  const nodes = useNodes<CommonNodeType>()
  const node = useMemo(() => {
    if (isSystemVar(valueSelector))
      return nodes.find(node => node.data.type === BlockEnum.Start)

    return nodes.find(node => node.id === valueSelector[0])
  }, [nodes, valueSelector])
  const isEnv = isENV(valueSelector)

  const variableName = isSystemVar(valueSelector) ? valueSelector.slice(0).join('.') : valueSelector.slice(1).join('.')
  const isException = isExceptionVariable(variableName, node?.data.type)

  return (
    <div className='inline-flex items-center px-1.5 max-w-full h-6 text-xs rounded-md border-[0.5px] border-[rgba(16, 2440,0.08)] bg-white shadow-xs'>
      {!isEnv && (
        <>
          {node && (
            <VarBlockIcon
              className='shrink-0 mr-0.5 text-text-secondary'
              type={node!.data.type}
            />
          )}
          <div
            className='max-w-[60px] truncate text-text-secondary font-medium'
            title={node?.data.title}
          >
            {node?.data.title}
          </div>
          <Line3 className='shrink-0 mx-0.5' />
          <Variable02 className={cn('shrink-0 mr-0.5 w-3.5 h-3.5 text-text-accent', isException && 'text-text-warning')} />
        </>
      )}
      {isEnv && <Env className='shrink-0 mr-0.5 w-3.5 h-3.5 text-util-colors-violet-violet-600' />}
      <div
        className={cn('truncate text-text-accent font-medium', isEnv && 'text-text-secondary', isException && 'text-text-warning')}
        title={variableName}
      >
        {variableName}
      </div>
      {
        varType && (
          <div className='shrink-0 ml-0.5 text-text-tertiary'>{capitalize(varType)}</div>
        )
      }
    </div>
  )
}

export default VariableTag
