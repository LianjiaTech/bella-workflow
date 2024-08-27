import type { FC } from 'react'
import React from 'react'
import type { ParallelNodeType } from './types'
import type { NodeProps } from '@/app/components/workflow/types'

const Node: FC<NodeProps<ParallelNodeType>> = () => {
  return (
    <div className='mb-1 px-3 py-1 space-y-0.5'>
    </div>
  )
}

export default React.memo(Node)
