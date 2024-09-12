import { useCallback } from 'react'
import type { IOtherOptions } from '@/service/base'
import { ssePost } from '@/service/base'

export const useCopilot = () => {
  const handleRun = useCallback(async (
    params: any,
    callback?: IOtherOptions,
  ) => {
    const {
      onWorkflowStarted,
      onWorkflowFinished,
      onNodeStarted,
      onNodeFinished,
      onIterationStart,
      onIterationNext,
      onIterationFinish,
      onError,
      ...restCallback
    } = callback || {}

    const url = '/capi/copilot/chat'
    ssePost(
      url,
      {
        body: params,
      },
      {
        onWorkflowStarted: (params) => {
          if (onWorkflowStarted)
            onWorkflowStarted(params)
        },
        onWorkflowFinished: (params) => {
          if (onWorkflowFinished)
            onWorkflowFinished(params)
        },
        onError: (params) => {
          if (onError)
            onError(params)
        },
        onNodeStarted: (params) => {
          if (onNodeStarted)
            onNodeStarted(params)
        },
        onNodeFinished: (params) => {
          if (onNodeFinished)
            onNodeFinished(params)
        },
        onIterationStart: (params) => {
          if (onIterationStart)
            onIterationStart(params)
        },
        onIterationNext: (params) => {
          if (onIterationNext)
            onIterationNext(params)
        },
        onIterationFinish: (params) => {
          if (onIterationFinish)
            onIterationFinish(params)
        },
        onTextChunk: (params) => {
        },
        onTextReplace: (params) => {
        },
        onTTSChunk: (messageId: string, audio: string, audioType?: string) => {
        },
        onTTSEnd: (messageId: string, audio: string, audioType?: string) => {
        },
        ...restCallback,
      },
    )
  }, [])

  const handleStopRun = useCallback((taskId: string) => {
  }, [])

  return {
    handleRun,
    handleStopRun,
  }
}
