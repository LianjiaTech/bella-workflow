import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'
import { useTranslation } from 'react-i18next'
import { produce, setAutoFreeze } from 'immer'
import { useWorkflowRun } from '../../hooks'
import { NodeRunningStatus, WorkflowRunningStatus } from '../../types'
import type {
  ChatItem,
  Inputs,
  PromptVariable,
} from '@/app/components/base/chat/types'
import { useToastContext } from '@/app/components/base/toast'
import { replaceStringWithValues } from '@/app/components/app/configuration/prompt-value-panel'
import type { FileEntity } from '@/app/components/base/file-uploader/types'
import type { ThoughtItem } from '@/app/components/base/chat/chat/type'

type GetAbortController = (abortController: AbortController) => void
type SendCallback = {
  onGetSuggestedQuestions?: (responseItemId: string, getAbortController: GetAbortController) => Promise<any>
}

type ChatItemT = ChatItem & {
  _isTemporary?: boolean
  onlyShowWorkflowProgress?: boolean // 标记这个项只用于显示工作流进度
}

export const useChat = (
  config: any,
  promptVariablesConfig?: {
    inputs: Inputs
    promptVariables: PromptVariable[]
  },
  prevChatList?: ChatItem[],
  stopChat?: (taskId: string) => void,
) => {
  const { t } = useTranslation()
  const { notify } = useToastContext()
  const { handleRun } = useWorkflowRun()
  const hasStopResponded = useRef(false)
  const connversationId = useRef('')
  const taskIdRef = useRef('')
  const [chatList, setChatList] = useState<ChatItem[]>(prevChatList || [])
  const chatListRef = useRef<ChatItem[]>(prevChatList || [])
  const [isResponding, setIsResponding] = useState(false)
  const isRespondingRef = useRef(false)
  const [suggestedQuestions, setSuggestQuestions] = useState<string[]>([])
  const suggestedQuestionsAbortControllerRef = useRef<AbortController | null>(null)
  // Add a Map to track message_id to ChatItem relationships
  const responseMapRef = useRef<Map<string, ChatItem>>(new Map())

  useEffect(() => {
    setAutoFreeze(false)
    return () => {
      setAutoFreeze(true)
    }
  }, [])

  // 统一的处理函数来管理ChatList
  const handleUpdateChatList = useCallback((newChatList: ChatItem[]) => {
    setChatList(newChatList)
    chatListRef.current = newChatList
  }, [])

  // 添加一个函数来移除临时项并更新列表
  const getFilteredChatList = useCallback(() => {
    return chatListRef.current.filter(item => !(item as ChatItemT)?._isTemporary)
  }, [])

  // 更新所有已有响应项的工作流状态
  const updateWorkflowProcessForResponses = useCallback((workflowProcess: any, workflow_run_id?: string) => {
    if (responseMapRef.current.size === 0)
      return

    handleUpdateChatList(produce(getFilteredChatList(), (draft) => {
      responseMapRef.current.forEach((item, messageId) => {
        const currentIndex = draft.findIndex(draftItem => draftItem.id === messageId)
        if (currentIndex >= 0) {
          draft[currentIndex] = {
            ...draft[currentIndex],
            workflowProcess,
            ...(workflow_run_id ? { workflow_run_id } : {}),
          }
        }
      })
    }))
  }, [])

  const handleResponding = useCallback((isResponding: boolean) => {
    setIsResponding(isResponding)
    isRespondingRef.current = isResponding
  }, [])

  const getIntroduction = useCallback((str: string) => {
    return replaceStringWithValues(str, promptVariablesConfig?.promptVariables || [], promptVariablesConfig?.inputs || {})
  }, [promptVariablesConfig?.inputs, promptVariablesConfig?.promptVariables])
  useEffect(() => {
    if (config?.opening_statement) {
      handleUpdateChatList(produce(chatListRef.current, (draft) => {
        const index = draft.findIndex(item => item.isOpeningStatement)

        if (index > -1) {
          draft[index] = {
            ...draft[index],
            content: getIntroduction(config.opening_statement),
            suggestedQuestions: config.suggested_questions,
          }
        }
        else {
          draft.unshift({
            id: `${Date.now()}`,
            content: getIntroduction(config.opening_statement),
            isAnswer: true,
            isOpeningStatement: true,
            suggestedQuestions: config.suggested_questions,
          })
        }
      }))
    }
  }, [config?.opening_statement, getIntroduction, config?.suggested_questions, handleUpdateChatList])

  const handleStop = useCallback(() => {
    hasStopResponded.current = true
    handleResponding(false)
    if (stopChat && taskIdRef.current)
      stopChat(taskIdRef.current)

    if (suggestedQuestionsAbortControllerRef.current)
      suggestedQuestionsAbortControllerRef.current.abort()
  }, [handleResponding, stopChat])

  const handleRestart = useCallback(() => {
    connversationId.current = ''
    taskIdRef.current = ''
    responseMapRef.current.clear()
    handleStop()
    const newChatList = config?.opening_statement
      ? [{
        id: `${Date.now()}`,
        content: config.opening_statement,
        isAnswer: true,
        isOpeningStatement: true,
        suggestedQuestions: config.suggested_questions,
      }]
      : []
    handleUpdateChatList(newChatList)
    setSuggestQuestions([])
  }, [
    config,
    handleStop,
    handleUpdateChatList,
  ])

  const updateCurrentQA = useCallback(({
    responseItem,
    questionId,
    placeholderAnswerId,
    questionItem,
  }: {
    responseItem: ChatItem
    questionId: string
    placeholderAnswerId: string
    questionItem: ChatItem
  }) => {
    const newListWithAnswer = produce(
      chatListRef.current.filter(item => item.id !== responseItem.id && item.id !== placeholderAnswerId),
      (draft) => {
        if (!draft.find(item => item.id === questionId))
          draft.push({ ...questionItem })

        draft.push({ ...responseItem })
      })
    handleUpdateChatList(newListWithAnswer)
  }, [handleUpdateChatList])

  const handleSend = useCallback((
    params: any,
    {
      onGetSuggestedQuestions,
    }: SendCallback,
  ) => {
    if (isRespondingRef.current) {
      notify({ type: 'info', message: t('appDebug.errorMessage.waitForResponse') })
      return false
    }

    // 清空上一轮对话的响应项，确保状态正确重置
    responseMapRef.current.clear()

    const questionId = `question-${Date.now()}`
    const questionItem = {
      id: questionId,
      content: params.query,
      isAnswer: false,
      message_files: params.files,
    }

    // Add the question to the chat list
    const newList = [...chatListRef.current, questionItem]

    const tempResponseId = `temp-response-${Date.now()}`
    const tempWorkflowProcess: ChatItem['workflowProcess'] = {
      status: WorkflowRunningStatus.Running,
      tracing: [],
    }

    const tempResponseItem: ChatItemT = {
      id: tempResponseId,
      content: '',
      agent_thoughts: [],
      message_files: [],
      isAnswer: true,
      workflowProcess: tempWorkflowProcess,
      _isTemporary: true,
    }

    const newListWithTemp = [...newList, tempResponseItem]
    handleUpdateChatList(newListWithTemp)

    let isInIteration = false

    handleResponding(true)

    const bodyParams = {
      conversation_id: connversationId.current,
      ...params,
    }
    if (bodyParams?.files?.length) {
      params.fileIds = bodyParams.files.map((item: FileEntity) => {
        return item.id
      })
      delete params.files
    }

    // No need for hasSetResponseId anymore as we're relying completely on backend IDs
    let sharedWorkflowProcess: ChatItem['workflowProcess']

    handleRun(
      params,
      {
        onData: (message: string, isFirstMessage: boolean, { conversationId: newConversationId, messageId, taskId, workflow_run_id }: any) => {
        // Process incoming message
          if (messageId) {
            let currentResponseItem = responseMapRef.current.get(messageId)

            if (currentResponseItem) {
            // We already have this message in our map, append to it
              currentResponseItem.content = currentResponseItem.content + message
            }
            else {
            // This is a new message ID, create a new response item
              currentResponseItem = {
                id: messageId,
                content: message,
                agent_thoughts: [],
                message_files: [],
                isAnswer: true,
                workflowProcess: sharedWorkflowProcess, // Share the workflow process
              }

              // Add to our map
              responseMapRef.current.set(messageId, currentResponseItem)

              // Add to our chat list - filter out any temporary chat items
              const filteredList = getFilteredChatList()
              const newList = [...filteredList, currentResponseItem]
              handleUpdateChatList(newList)
            }
          }

          if (isFirstMessage && newConversationId)
            connversationId.current = newConversationId

          taskIdRef.current = taskId

          // For all messages, update them in the chat list
          if (messageId) {
            const currentResponseItem = responseMapRef.current.get(messageId)
            if (currentResponseItem) {
              handleUpdateChatList(produce(chatListRef.current, (draft) => {
                const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
                if (currentIndex >= 0) {
                  draft[currentIndex] = {
                    ...draft[currentIndex],
                    ...currentResponseItem,
                  }
                }
              }))
            }
          }
        },
        onThought: (thought: ThoughtItem) => {
          if (!thought.message_id)
            return

          // Find or create the response item for this message
          let currentResponse = responseMapRef.current.get(thought.message_id)

          if (!currentResponse) {
            // Create a new response if none exists
            currentResponse = {
              id: thought.message_id,
              content: '',
              agent_thoughts: [],
              message_files: [],
              isAnswer: true,
              workflowProcess: sharedWorkflowProcess,
            }
            responseMapRef.current.set(thought.message_id, currentResponse)

            // Add to chat list
            const filteredList = getFilteredChatList()
            const newList = [...filteredList, currentResponse]
            handleUpdateChatList(newList)
          }

          // Update thoughts
          if (!currentResponse.agent_thoughts)
            currentResponse.agent_thoughts = []

          if (currentResponse.agent_thoughts.length === 0) {
            currentResponse.agent_thoughts.push(thought)
          }
          else {
            const lastThought = currentResponse.agent_thoughts[currentResponse.agent_thoughts.length - 1]
            // thought changed but still the same thought, so update.
            if (lastThought.id === thought.id) {
              thought.thought = lastThought.thought + thought.thought
              thought.message_files = lastThought.message_files
              currentResponse.agent_thoughts[currentResponse.agent_thoughts.length - 1] = thought
            }
            else {
              currentResponse.agent_thoughts.push(thought)
            }
          }

          // Update in the chat list
          handleUpdateChatList(produce(chatListRef.current, (draft) => {
            const currentIndex = draft.findIndex(item => item.id === currentResponse!.id)
            if (currentIndex >= 0) {
              draft[currentIndex] = {
                ...draft[currentIndex],
                ...currentResponse,
              }
            }
          }))
        },
        async onCompleted(hasError?: boolean, errorMessage?: string) {
          handleResponding(false)

          if (hasError) {
            if (errorMessage) {
              // Update all responses in the map with the error
              responseMapRef.current.forEach((item) => {
                item.content = errorMessage
                item.isError = true
              })

              // Update the chat list to reflect errors
              const newListWithAnswer = produce(
                chatListRef.current.filter((item) => {
                  // Keep items that aren't in our response map
                  const isInResponseMap = Array.from(responseMapRef.current.keys()).includes(item.id)
                  return !isInResponseMap
                }),
                (draft) => {
                  if (!draft.find(item => item.id === questionId))
                    draft.push({ ...questionItem })

                  // Add all responses from the map
                  responseMapRef.current.forEach((item) => {
                    draft.push({ ...item })
                  })
                })
              handleUpdateChatList(newListWithAnswer)
            }
            return
          }

          if (config?.suggested_questions_after_answer?.enabled && !hasStopResponded.current && onGetSuggestedQuestions) {
            // Use the ID of the first response for suggested questions
            const firstResponseId = responseMapRef.current.values().next().value?.id
            if (firstResponseId) {
              const { data }: any = await onGetSuggestedQuestions(
                firstResponseId,
                newAbortController => suggestedQuestionsAbortControllerRef.current = newAbortController,
              )
              setSuggestQuestions(data)
            }
          }
        },

        onMessageEnd: (messageEnd) => {
          // Find the response that matches this message ID
          const targetResponse = responseMapRef.current.get(messageEnd.id)
          if (!targetResponse)
            return
          targetResponse.citation = messageEnd.metadata?.retriever_resources || []

          const newListWithAnswer = produce(
            chatListRef.current.filter((item) => {
              // Keep items that aren't in our response map
              const isInResponseMap = Array.from(responseMapRef.current.keys()).includes(item.id)
              return !isInResponseMap
            }),
            (draft) => {
              if (!draft.find(item => item.id === questionId))
                draft.push({ ...questionItem })

              // Add all responses from the map
              responseMapRef.current.forEach((item) => {
                draft.push({ ...item })
              })
            })
          handleUpdateChatList(newListWithAnswer)
        },

        onMessageReplace: (messageReplace) => {
          // Find the response that matches this message ID
          const targetResponse = responseMapRef.current.get(messageReplace.id)
          if (targetResponse) {
            targetResponse.content = messageReplace.answer

            // Update in the chat list
            handleUpdateChatList(produce(chatListRef.current, (draft) => {
              const currentIndex = draft.findIndex(item => item.id === targetResponse.id)
              if (currentIndex >= 0) {
                draft[currentIndex] = {
                  ...draft[currentIndex],
                  ...targetResponse,
                }
              }
            }))
          }
        },

        onError() {
          handleResponding(false)
        },
        onWorkflowStarted: ({ workflow_run_id, task_id }) => {
          taskIdRef.current = task_id

          // Create a shared workflow process for all messages
          sharedWorkflowProcess = {
            status: WorkflowRunningStatus.Running,
            tracing: [],
          }

          // 更新已有响应项的工作流状态
          updateWorkflowProcessForResponses(sharedWorkflowProcess, workflow_run_id)
        },
        onWorkflowFinished: ({ data }) => {
          // Update the shared workflow process status
          if (sharedWorkflowProcess)
            sharedWorkflowProcess.status = data.status as WorkflowRunningStatus

          // 检查是否有任何消息响应
          const hasMessages = responseMapRef.current.size > 0

          if (!hasMessages) {
            // 如果没有任何消息响应，创建一个只包含WorkflowProgress的ChatItem
            const workflowProgressItem: ChatItemT = {
              id: `workflow-progress-${Date.now()}`,
              content: '', // 可以为空，因为我们只关心WorkflowProgress
              agent_thoughts: [],
              message_files: [],
              isAnswer: true,
              _isTemporary: false, // 这不是临时项，因为它是最终结果
              workflowProcess: sharedWorkflowProcess,
              workflow_run_id: data.workflow_id,
              onlyShowWorkflowProgress: true, // 标记这个项只用于显示工作流进度
            }

            // 移除任何临时项并添加工作流进度项
            handleUpdateChatList([...getFilteredChatList(), workflowProgressItem as ChatItem])
          }
          else {
            // 如果有消息响应，更新它们的工作流程状态
            updateWorkflowProcessForResponses(sharedWorkflowProcess)
          }
        },
        onIterationStart: ({ data }) => {
          if (sharedWorkflowProcess && sharedWorkflowProcess.tracing) {
            sharedWorkflowProcess.tracing.push({
              ...data,
              status: NodeRunningStatus.Running,
              details: [],
            } as any)

            // Update all responses that use this workflow process
            updateWorkflowProcessForResponses(sharedWorkflowProcess)

            isInIteration = true
          }
        },
        onIterationNext: () => {
          if (sharedWorkflowProcess && sharedWorkflowProcess.tracing && sharedWorkflowProcess.tracing.length > 0) {
            const tracing = sharedWorkflowProcess.tracing
            const iterations = tracing[tracing.length - 1]
            if (iterations.details) {
              iterations.details.push([])

              // Update all responses that use this workflow process
              handleUpdateChatList(produce(chatListRef.current, (draft) => {
                responseMapRef.current.forEach((item, messageId) => {
                  const currentIndex = draft.findIndex(draftItem => draftItem.id === messageId)
                  if (currentIndex >= 0) {
                    draft[currentIndex] = {
                      ...draft[currentIndex],
                      workflowProcess: sharedWorkflowProcess,
                    }
                  }
                })
              }))
            }
          }
        },
        onIterationFinish: ({ data }) => {
          if (sharedWorkflowProcess && sharedWorkflowProcess.tracing && sharedWorkflowProcess.tracing.length > 0) {
            const tracing = sharedWorkflowProcess.tracing
            const iterations = tracing[tracing.length - 1]
            tracing[tracing.length - 1] = {
              ...iterations,
              ...data,
              status: NodeRunningStatus.Succeeded,
            } as any

            // Update all responses that use this workflow process
            updateWorkflowProcessForResponses(sharedWorkflowProcess)

            isInIteration = false
          }
        },
        onNodeStarted: ({ data }) => {
          if (!sharedWorkflowProcess || !sharedWorkflowProcess.tracing)
            return

          if (isInIteration) {
            const tracing = sharedWorkflowProcess.tracing
            if (tracing.length > 0) {
              const iterations = tracing[tracing.length - 1]
              if (iterations.details && iterations.details.length > 0) {
                const currIteration = iterations.details[iterations.details.length - 1]
                currIteration.push({
                  ...data,
                  status: NodeRunningStatus.Running,
                } as any)

                // Update all responses that use this workflow process
                handleUpdateChatList(produce(chatListRef.current, (draft) => {
                  responseMapRef.current.forEach((item, messageId) => {
                    const currentIndex = draft.findIndex(draftItem => draftItem.id === messageId)
                    if (currentIndex >= 0) {
                      draft[currentIndex] = {
                        ...draft[currentIndex],
                        workflowProcess: sharedWorkflowProcess,
                      }
                    }
                  })
                }))
              }
            }
          }
          else {
            sharedWorkflowProcess.tracing.push({
              ...data,
              status: NodeRunningStatus.Running,
            } as any)

            // Update all responses that use this workflow process
            updateWorkflowProcessForResponses(sharedWorkflowProcess)
          }
        },
        onNodeFinished: ({ data }) => {
          if (!sharedWorkflowProcess || !sharedWorkflowProcess.tracing)
            return

          if (isInIteration) {
            const tracing = sharedWorkflowProcess.tracing
            if (tracing.length > 0) {
              const iterations = tracing[tracing.length - 1]
              if (iterations.details && iterations.details.length > 0) {
                const currIteration = iterations.details[iterations.details.length - 1]
                if (currIteration.length > 0) {
                  currIteration[currIteration.length - 1] = {
                    ...data,
                    status: NodeRunningStatus.Succeeded,
                  } as any

                  // Update all responses that use this workflow process
                  handleUpdateChatList(produce(chatListRef.current, (draft) => {
                    responseMapRef.current.forEach((item, messageId) => {
                      const currentIndex = draft.findIndex(draftItem => draftItem.id === messageId)
                      if (currentIndex >= 0) {
                        draft[currentIndex] = {
                          ...draft[currentIndex],
                          workflowProcess: sharedWorkflowProcess,
                        }
                      }
                    })
                  }))
                }
              }
            }
          }
          else {
            const currentIndex = sharedWorkflowProcess.tracing.findIndex(item => item.node_id === data.node_id)
            if (currentIndex >= 0) {
              const extras = sharedWorkflowProcess.tracing[currentIndex].extras
                ? { extras: sharedWorkflowProcess.tracing[currentIndex].extras }
                : {}

              sharedWorkflowProcess.tracing[currentIndex] = {
                ...extras,
                ...data,
                status: NodeRunningStatus.Succeeded,
              } as any

              // Update all responses that use this workflow process
              handleUpdateChatList(produce(chatListRef.current, (draft) => {
                responseMapRef.current.forEach((item, messageId) => {
                  const currentIndex = draft.findIndex(draftItem => draftItem.id === messageId)
                  if (currentIndex >= 0) {
                    draft[currentIndex] = {
                      ...draft[currentIndex],
                      workflowProcess: sharedWorkflowProcess,
                    }
                  }
                })
              }))
            }
          }
        },
      },
    )
  }, [handleRun, handleResponding, handleUpdateChatList, notify, t, updateCurrentQA, config.suggested_questions_after_answer?.enabled])

  return {
    conversationId: connversationId.current,
    chatList,
    handleSend,
    handleStop,
    handleRestart,
    isResponding,
    suggestedQuestions,
  }
}
