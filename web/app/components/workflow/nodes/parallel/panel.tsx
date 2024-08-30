import { type FC } from 'react'
import React from 'react'
import type { ParallelNodeType } from './types'
import { type NodePanelProps } from '@/app/components/workflow/types'

const Panel: FC<NodePanelProps<ParallelNodeType>> = () => {
  return (
    <div className='mt-2'>
      <div className='px-4 pb-4 space-y-4'>
      </div>
    </div>
  )
}

export default React.memo(Panel)
