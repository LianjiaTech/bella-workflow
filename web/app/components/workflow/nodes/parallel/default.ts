import { BlockEnum } from '../../types'
import type { NodeDefault } from '../../types'
import { type ParallelNodeType } from './types'
import { ALL_CHAT_AVAILABLE_BLOCKS, ALL_COMPLETION_AVAILABLE_BLOCKS } from '@/app/components/workflow/constants'

const nodeDefault: NodeDefault<ParallelNodeType> = {
  defaultValue: {
  },
  getAvailablePrevNodes(isChatMode: boolean) {
    const nodes = isChatMode
      ? ALL_CHAT_AVAILABLE_BLOCKS.filter(type => type !== BlockEnum.End)
      : ALL_COMPLETION_AVAILABLE_BLOCKS.filter(type => type !== BlockEnum.End)
    return nodes
  },
  getAvailableNextNodes(isChatMode: boolean) {
    const nodes = isChatMode
      ? ALL_CHAT_AVAILABLE_BLOCKS.filter(type => type !== BlockEnum.End)
      : ALL_COMPLETION_AVAILABLE_BLOCKS.filter(type => type !== BlockEnum.End)
    return nodes
  },
  checkValid() {
    return {
      isValid: true,
    }
  },
}

export default nodeDefault
