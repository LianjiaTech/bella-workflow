import useNodeInfo from './use-node-info'
import {
  useIsChatMode,
  useWorkflow,
  useWorkflowVariables,
} from '@/app/components/workflow/hooks'
import type { ValueSelector, Var } from '@/app/components/workflow/types'
type Params = {
  onlyLeafNodeVar?: boolean
  filterVar: (payload: Var, selector: ValueSelector) => boolean
}

const useAvailableVarList = (nodeId: string, {
  onlyLeafNodeVar,
  filterVar,
}: Params = {
  onlyLeafNodeVar: false,
  filterVar: () => true,
}) => {
  const { getTreeLeafNodes, getBeforeNodesInSameBranch, getNode } = useWorkflow()
  const { getNodeAvailableVars } = useWorkflowVariables()
  const isChatMode = useIsChatMode()

  const availableNodes = onlyLeafNodeVar ? getTreeLeafNodes(nodeId) : getBeforeNodesInSameBranch(nodeId)

  const {
    parentNode: iterationNode,
  } = useNodeInfo(nodeId)

  const currentNode = getNode(nodeId)
  const availableVars = getNodeAvailableVars({
    parentNode: iterationNode,
    beforeNodes: availableNodes,
    isChatMode,
    filterVar,
    currentNode,
  })
  if (currentNode)
    availableNodes.unshift(currentNode)

  return {
    availableVars,
    availableNodes,
    availableNodesWithParent: iterationNode ? [...availableNodes, iterationNode] : availableNodes,
  }
}

export default useAvailableVarList
