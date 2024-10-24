import { useMemo } from 'react'
import { useStore } from '../store'

export const useWorkflowMode = () => {
  const historyWorkflowData = useStore(s => s.historyWorkflowData)
  const isRestoring = useStore(s => s.isRestoring)
  const isVersionHistory = useStore(s => s.isVersionHistory)
  return useMemo(() => {
    return {
      normal: !historyWorkflowData && !isRestoring && !isVersionHistory,
      restoring: isRestoring,
      viewHistory: !!historyWorkflowData,
      versionHistory: isVersionHistory,
    }
  }, [isVersionHistory, historyWorkflowData, isRestoring])
}
