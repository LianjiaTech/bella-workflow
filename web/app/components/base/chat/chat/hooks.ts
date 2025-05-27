import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'
import { useTranslation } from 'react-i18next'
import { produce, setAutoFreeze } from 'immer'
import { useParams, usePathname } from 'next/navigation'
import { v4 as uuidV4 } from 'uuid'
import type {
  ChatConfig,
  ChatItem,
  Inputs,
  PromptVariable,
} from '../types'
import { useToastContext } from '@/app/components/base/toast'
import { ssePost } from '@/service/base'
import { replaceStringWithValues } from '@/app/components/app/configuration/prompt-value-panel'
import type { Annotation } from '@/models/log'
import { WorkflowRunningStatus } from '@/app/components/workflow/types'
import useTimestamp from '@/hooks/use-timestamp'
import { AudioPlayerManager } from '@/app/components/base/audio-btn/audio.player.manager'
import type { FileEntity } from '@/app/components/base/file-uploader/types'

type GetAbortController = (abortController: AbortController) => void
type SendCallback = {
  onGetConvesationMessages?: (conversationId: string, getAbortController: GetAbortController) => Promise<any>
  onGetSuggestedQuestions?: (responseItemId: string, getAbortController: GetAbortController) => Promise<any>
  onConversationComplete?: (conversationId: string) => void
  isPublicAPI?: boolean
}

type ChatItemT = ChatItem & {
  _isTemporary?: boolean
  onlyShowWorkflowProgress?: boolean // 标记这个项只用于显示工作流进度
}

export const useCheckPromptVariables = () => {
  const { t } = useTranslation()
  const { notify } = useToastContext()

  const checkPromptVariables = useCallback((promptVariablesConfig: {
    inputs: Inputs
    promptVariables: PromptVariable[]
  }) => {
    const {
      promptVariables,
      inputs,
    } = promptVariablesConfig
    let hasEmptyInput = ''
    const requiredVars = promptVariables.filter(({ key, name, required, type }) => {
      if (type !== 'string' && type !== 'paragraph' && type !== 'select')
        return false
      const res = (!key || !key.trim()) || (!name || !name.trim()) || (required || required === undefined || required === null)
      return res
    })

    if (requiredVars?.length) {
      requiredVars.forEach(({ key, name }) => {
        if (hasEmptyInput)
          return

        if (!inputs[key])
          hasEmptyInput = name
      })
    }

    if (hasEmptyInput) {
      notify({ type: 'error', message: t('appDebug.errorMessage.valueOfVarRequired', { key: hasEmptyInput }) })
      return false
    }
  }, [notify, t])

  return checkPromptVariables
}

export const useChat = (
  config?: ChatConfig,
  promptVariablesConfig?: {
    inputs: Inputs
    promptVariables: PromptVariable[]
  },
  prevChatList?: ChatItem[],
  stopChat?: (taskId: string) => void,
) => {
  const { t } = useTranslation()
  const { formatTime } = useTimestamp()
  const { notify } = useToastContext()
  const connversationId = useRef('')
  const hasStopResponded = useRef(false)
  const [isResponding, setIsResponding] = useState(false)
  const isRespondingRef = useRef(false)
  const [chatList, setChatList] = useState<ChatItem[]>(prevChatList || [])
  const chatListRef = useRef<ChatItem[]>(prevChatList || [])
  const taskIdRef = useRef('')
  const [suggestedQuestions, setSuggestQuestions] = useState<string[]>([])
  const conversationMessagesAbortControllerRef = useRef<AbortController | null>(null)
  const suggestedQuestionsAbortControllerRef = useRef<AbortController | null>(null)
  const checkPromptVariables = useCheckPromptVariables()
  const params = useParams()
  const pathname = usePathname()
  const responseMapRef = useRef<Map<string, ChatItem>>(new Map())
  // 定义变量用于跟踪是否接收到数据
  const hasReceivedDataRef = useRef(false)
  // 定义变量用于跟踪临时响应ID
  const tempResponseIdRef = useRef('')
  // 定义变量用于跟踪是否在迭代中
  const isInIteration = false
  useEffect(() => {
    setAutoFreeze(false)
    return () => {
      setAutoFreeze(true)
    }
  }, [])

  const handleUpdateChatList = useCallback((newChatList: ChatItem[]) => {
    setChatList(newChatList)
    chatListRef.current = newChatList
  }, [])

  // 添加一个函数来移除临时项并更新列表
  const getFilteredChatList = useCallback(() => {
    return chatListRef.current.filter(item => !(item as ChatItemT)?._isTemporary)
  }, [])
  const handleResponding = useCallback((isResponding: boolean) => {
    setIsResponding(isResponding)
    isRespondingRef.current = isResponding

    // 重置数据接收状态
    if (isResponding) {
      hasReceivedDataRef.current = false
      tempResponseIdRef.current = ''
    }
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
    if (conversationMessagesAbortControllerRef.current)
      conversationMessagesAbortControllerRef.current.abort()
    if (suggestedQuestionsAbortControllerRef.current)
      suggestedQuestionsAbortControllerRef.current.abort()
  }, [stopChat, handleResponding])

  const handleRestart = useCallback(() => {
    connversationId.current = ''
    taskIdRef.current = ''
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

  const handleSend = useCallback(async (
    url: string,
    data: any,
    {
      onGetConvesationMessages,
      onGetSuggestedQuestions,
      onConversationComplete,
      isPublicAPI,
    }: SendCallback,
  ) => {
    setSuggestQuestions([])

    if (isRespondingRef.current) {
      notify({ type: 'info', message: t('appDebug.errorMessage.waitForResponse') })
      return false
    }

    if (promptVariablesConfig?.inputs && promptVariablesConfig?.promptVariables)
      checkPromptVariables(promptVariablesConfig)

    const questionId = `question-${Date.now()}`
    const questionItem = {
      id: questionId,
      content: data.query,
      isAnswer: false,
      message_files: data.files,
    }

    const placeholderAnswerId = `answer-placeholder-${Date.now()}`
    const placeholderAnswerItem = {
      id: placeholderAnswerId,
      content: '',
      isAnswer: true,
    }

    const newList = [...chatListRef.current, questionItem, placeholderAnswerItem]
    handleUpdateChatList(newList)

    // answer
    let responseItem: ChatItem = {
      id: placeholderAnswerId,
      content: '',
      agent_thoughts: [],
      message_files: [],
      isAnswer: true,
    }

    let isInIteration = false

    handleResponding(true)
    hasStopResponded.current = false

    const bodyParams = {
      response_mode: 'streaming',
      conversation_id: connversationId.current,
      ...data,
    }
    if (bodyParams?.files?.length) {
      params.fileIds = bodyParams.files.map((item: FileEntity) => {
        return item.id
      })
      delete params.files
    }

    let isAgentMode = false
    let hasSetResponseId = false

    let ttsUrl = ''
    let ttsIsPublic = false
    if (params.token) {
      ttsUrl = '/text-to-audio'
      ttsIsPublic = true
    }
    else if (params.appId) {
      if (pathname.search('explore/installed') > -1)
        ttsUrl = `/installed-apps/${params.appId}/text-to-audio`
      else
        ttsUrl = `/apps/${params.appId}/text-to-audio`
    }
    const player = AudioPlayerManager.getInstance().getAudioPlayer(ttsUrl, ttsIsPublic, uuidV4(), 'none', 'none', (_: any): any => {})
    ssePost(
      url,
      {
        body: bodyParams,
      },
      {
        isPublicAPI,
        onData: (message: string, isFirstMessage: boolean, { conversationId: newConversationId, messageId, taskId }: any) => {
          // 标记已接收到真实数据
          hasReceivedDataRef.current = true
          if (messageId && messageId !== responseItem.id) {
            // 检查是否已经存在此消息的响应项
            const existingItem = responseMapRef.current.get(messageId)
            if (existingItem) {
              responseItem = existingItem
            }
            else {
              responseItem = {
                id: messageId,
                content: '',
                agent_thoughts: [],
                message_files: [],
                isAnswer: true,
              }
              responseMapRef.current.set(messageId, responseItem)
            }
          }

          if (!isAgentMode) {
            responseItem.content = responseItem.content + message
          }
          else {
            const lastThought = responseItem.agent_thoughts?.[responseItem.agent_thoughts?.length - 1]
            if (lastThought)
              lastThought.thought = lastThought.thought + message // need immer setAutoFreeze
          }

          if (messageId && !hasSetResponseId) {
            responseItem.id = messageId
            hasSetResponseId = true
          }

          if (isFirstMessage && newConversationId)
            connversationId.current = newConversationId

          taskIdRef.current = taskId
          if (messageId)
            responseItem.id = messageId

          updateCurrentQA({
            responseItem,
            questionId,
            placeholderAnswerId,
            questionItem,
          })
        },
        onCompleted: (hasError?: boolean, messageId?: string) => {
          handleResponding(false)

          if (hasError)
            return

          if (onConversationComplete)
            onConversationComplete(connversationId.current)

          if (connversationId.current && !hasStopResponded.current && onGetConvesationMessages) {
            const getMessages = async () => {
              const { data }: any = await onGetConvesationMessages(
                connversationId.current,
                newAbortController => conversationMessagesAbortControllerRef.current = newAbortController,
              )

              if (messageId) {
                const newResponseItem = data.find((item: any) => item.id === messageId)
                if (!newResponseItem)
                  return

                const newChatList = produce(chatListRef.current, (draft) => {
                  const index = draft.findIndex(item => item.id === messageId)
                  if (index !== -1) {
                    const requestion = draft[index - 1]
                    draft[index - 1] = {
                      ...requestion,
                    }
                    draft[index] = {
                      ...draft[index],
                      content: newResponseItem.answer,
                      log: [
                        ...newResponseItem.message,
                        ...(newResponseItem.message[newResponseItem.message.length - 1].role !== 'assistant'
                          ? [
                            {
                              role: 'assistant',
                              text: newResponseItem.answer,
                              files: newResponseItem.message_files?.filter((file: any) => file.belongs_to === 'assistant') || [],
                            },
                          ]
                          : []),
                      ],
                      more: {
                        time: formatTime(newResponseItem.created_at, 'hh:mm A'),
                        tokens: newResponseItem.answer_tokens + newResponseItem.message_tokens,
                        latency: newResponseItem.provider_response_latency.toFixed(2),
                      },
                      // for agent log
                      conversationId: connversationId.current,
                      input: {
                        inputs: newResponseItem.inputs,
                        query: newResponseItem.query,
                      },
                    }
                  }
                })
                handleUpdateChatList(newChatList)
              }
            }
            getMessages()
          }
          if (config?.suggested_questions_after_answer?.enabled && !hasStopResponded.current && onGetSuggestedQuestions && messageId) {
            const getSuggestions = async () => {
              const { data }: any = await onGetSuggestedQuestions(
                messageId,
                newAbortController => suggestedQuestionsAbortControllerRef.current = newAbortController,
              )
              setSuggestQuestions(data)
            }
            getSuggestions()
          }
        },
        onFile: (file: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId) || responseItem
          const lastThought = currentResponseItem.agent_thoughts?.[currentResponseItem.agent_thoughts?.length - 1]
          if (lastThought)
            currentResponseItem.agent_thoughts![currentResponseItem.agent_thoughts!.length - 1].message_files = [...(lastThought as any).message_files, file]

          updateCurrentQA({
            responseItem: currentResponseItem,
            questionId,
            placeholderAnswerId,
            questionItem,
          })
        },
        onThought: (thought: any) => {
          isAgentMode = true
          let responseToUpdate: ChatItem

          if (thought.message_id) {
            const existingItem = responseMapRef.current.get(thought.message_id)
            if (existingItem) {
              responseToUpdate = existingItem
            }
            else {
              responseToUpdate = {
                id: thought.message_id,
                content: '',
                agent_thoughts: [],
                message_files: [],
                isAnswer: true,
              }
              responseMapRef.current.set(thought.message_id, responseToUpdate)
              if (!hasSetResponseId) {
                responseItem = responseToUpdate
                hasSetResponseId = true
              }
            }

            if (!responseToUpdate.agent_thoughts)
              responseToUpdate.agent_thoughts = []

            if (responseToUpdate.agent_thoughts.length === 0) {
              responseToUpdate.agent_thoughts.push(thought)
            }
            else {
              const lastThought = responseToUpdate.agent_thoughts[responseToUpdate.agent_thoughts.length - 1]
              // thought changed but still the same thought, so update.
              if (lastThought.id === thought.id) {
                thought.thought = lastThought.thought + thought.thought
                thought.message_files = lastThought.message_files
                responseToUpdate.agent_thoughts[responseToUpdate.agent_thoughts.length - 1] = thought
              }
              else {
                responseToUpdate.agent_thoughts.push(thought)
              }
            }

            updateCurrentQA({
              responseItem: responseToUpdate,
              questionId,
              placeholderAnswerId,
              questionItem,
            })
          }
        },
        onMessageEnd: (messageEnd: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId) || responseItem

          if (messageEnd.metadata?.annotation_reply) {
            currentResponseItem.id = messageEnd.id
            currentResponseItem.annotation = ({
              id: messageEnd.metadata.annotation_reply.id,
              authorName: messageEnd.metadata.annotation_reply.account.name,
            })
            const baseState = chatListRef.current.filter(item => item.id !== currentResponseItem.id && item.id !== placeholderAnswerId)
            const newListWithAnswer = produce(
              baseState,
              (draft) => {
                if (!draft.find(item => item.id === questionId))
                  draft.push({ ...questionItem })

                draft.push({
                  ...currentResponseItem,
                })
              })
            handleUpdateChatList(newListWithAnswer)
            return
          }
          currentResponseItem.citation = messageEnd.metadata?.retriever_resources || []

          const newListWithAnswer = produce(
            chatListRef.current.filter(item => item.id !== currentResponseItem.id && item.id !== placeholderAnswerId),
            (draft) => {
              if (!draft.find(item => item.id === questionId))
                draft.push({ ...questionItem })

              draft.push({ ...currentResponseItem })
            })
          handleUpdateChatList(newListWithAnswer)
        },
        onMessageReplace: (messageReplace: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId)
          if (currentResponseItem)
            currentResponseItem.content = messageReplace.answer
        },
        onError: () => {
          handleResponding(false)
          const newChatList = produce(chatListRef.current, (draft) => {
            draft.splice(draft.findIndex(item => item.id === placeholderAnswerId), 1)
          })
          handleUpdateChatList(newChatList)
        },
        onWorkflowStarted: ({ workflow_run_id, task_id }: any, messageId?: string) => {
          taskIdRef.current = task_id

          // 检查是否已经有消息响应
          const hasMessages = responseMapRef.current.size > 0

          if (!hasMessages) {
            // 如果没有消息响应，创建一个临时的工作流进度项
            const workflowProgressItem: ChatItemT = {
              id: `workflow-progress-temp-${Date.now()}`,
              content: '',
              agent_thoughts: [],
              message_files: [],
              isAnswer: true,
              _isTemporary: true, // 这是一个临时项
              workflowProcess: {
                status: WorkflowRunningStatus.Running,
                tracing: [],
              },
              workflow_run_id,
              onlyShowWorkflowProgress: true,
            }

            // 添加临时项到聊天列表
            handleUpdateChatList([...getFilteredChatList(), workflowProgressItem as ChatItem])
            return
          }

          // 如果提供了特定的消息ID
          if (messageId) {
            const currentResponseItem = responseMapRef.current.get(messageId)
            if (!currentResponseItem)
              return

            // 为此消息创建工作流进程
            currentResponseItem.workflowProcess = {
              status: WorkflowRunningStatus.Running,
              tracing: [],
            }
            currentResponseItem.workflow_run_id = workflow_run_id

            // 更新聊天列表
            handleUpdateChatList(produce(chatListRef.current, (draft) => {
              const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
              if (currentIndex !== -1) {
                draft[currentIndex] = {
                  ...draft[currentIndex],
                  ...currentResponseItem,
                }
              }
            }))
          }
          else {
            // 如果没有提供特定消息ID，更新所有消息的工作流状态
            handleUpdateChatList(produce(getFilteredChatList(), (draft) => {
              responseMapRef.current.forEach((item) => {
                // 为每个消息创建工作流进程
                item.workflowProcess = {
                  status: WorkflowRunningStatus.Running,
                  tracing: [],
                }
                item.workflow_run_id = workflow_run_id

                const currentIndex = draft.findIndex(draftItem => draftItem.id === item.id)
                if (currentIndex >= 0) {
                  draft[currentIndex] = {
                    ...draft[currentIndex],
                    workflowProcess: item.workflowProcess,
                    workflow_run_id,
                  }
                }
              })
            }))
          }
        },
        onWorkflowFinished: ({ data }: any, messageId?: string) => {
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
              workflowProcess: {
                status: data.status as WorkflowRunningStatus,
                tracing: [],
              },
              workflow_run_id: data.workflow_id,
              onlyShowWorkflowProgress: true, // 标记这个项只用于显示工作流进度
            }

            // 移除任何临时项并添加工作流进度项
            handleUpdateChatList([...getFilteredChatList(), workflowProgressItem as ChatItem])
            return
          }

          // 如果提供了特定的消息ID
          if (messageId) {
            const currentResponseItem = responseMapRef.current.get(messageId)
            if (!currentResponseItem || !currentResponseItem.workflowProcess)
              return

            // 更新工作流状态
            currentResponseItem.workflowProcess.status = data.status as WorkflowRunningStatus

            // 更新聊天列表
            handleUpdateChatList(produce(chatListRef.current, (draft) => {
              const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
              if (currentIndex !== -1) {
                draft[currentIndex] = {
                  ...draft[currentIndex],
                  ...currentResponseItem,
                }
              }
            }))
          }
          else {
            // 如果没有提供特定消息ID，更新所有消息的工作流状态
            handleUpdateChatList(produce(getFilteredChatList(), (draft) => {
              responseMapRef.current.forEach((item) => {
                if (item.workflowProcess) {
                  item.workflowProcess.status = data.status as WorkflowRunningStatus

                  const currentIndex = draft.findIndex(draftItem => draftItem.id === item.id)
                  if (currentIndex >= 0) {
                    draft[currentIndex] = {
                      ...draft[currentIndex],
                      workflowProcess: item.workflowProcess,
                    }
                  }
                }
              })
            }))
          }
        },
        onIterationStart: ({ data }: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId)
          if (!currentResponseItem || !currentResponseItem.workflowProcess || !currentResponseItem.workflowProcess.tracing)
            return

          currentResponseItem.workflowProcess.tracing.push({
            ...data,
            status: WorkflowRunningStatus.Running,
            details: [],
          } as any)
          handleUpdateChatList(produce(chatListRef.current, (draft) => {
            const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
            if (currentIndex !== -1) {
              draft[currentIndex] = {
                ...draft[currentIndex],
                ...currentResponseItem,
              }
            }
          }))
          isInIteration = true
        },
        onIterationFinish: ({ data }: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId)
          if (!currentResponseItem || !currentResponseItem.workflowProcess || !currentResponseItem.workflowProcess.tracing || currentResponseItem.workflowProcess.tracing.length === 0)
            return

          // 获取跟踪数组和迭代项
          const tracing = currentResponseItem.workflowProcess.tracing
          const iterations = tracing[tracing.length - 1]

          // 更新迭代状态
          tracing[tracing.length - 1] = {
            ...iterations,
            ...data,
            status: WorkflowRunningStatus.Succeeded,
          } as any

          // 更新聊天列表
          handleUpdateChatList(produce(chatListRef.current, (draft) => {
            const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
            if (currentIndex !== -1) {
              draft[currentIndex] = {
                ...draft[currentIndex],
                ...currentResponseItem,
              }
            }
          }))

          // 重置迭代状态
          isInIteration = false
        },
        onIterationNext: (data: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId)
          if (!currentResponseItem || !currentResponseItem.workflowProcess || !currentResponseItem.workflowProcess.tracing || currentResponseItem.workflowProcess.tracing.length === 0)
            return

          const tracing = currentResponseItem.workflowProcess.tracing
          const iterations = tracing[tracing.length - 1]

          if (iterations.details) {
            iterations.details.push([])

            // 更新聊天列表
            handleUpdateChatList(produce(chatListRef.current, (draft) => {
              const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
              if (currentIndex !== -1) {
                draft[currentIndex] = {
                  ...draft[currentIndex],
                  ...currentResponseItem,
                }
              }
            }))
          }
        },
        onNodeStarted: ({ data }: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId)
          if (!currentResponseItem || !currentResponseItem.workflowProcess || !currentResponseItem.workflowProcess.tracing)
            return

          if (isInIteration) {
            const tracing = currentResponseItem.workflowProcess.tracing
            if (tracing.length > 0) {
              const iterations = tracing[tracing.length - 1]
              if (iterations.details && iterations.details.length > 0) {
                const currIteration = iterations.details[iterations.details.length - 1]
                currIteration.push({
                  ...data,
                  status: WorkflowRunningStatus.Running,
                } as any)

                handleUpdateChatList(produce(chatListRef.current, (draft) => {
                  const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
                  if (currentIndex !== -1) {
                    draft[currentIndex] = {
                      ...draft[currentIndex],
                      ...currentResponseItem,
                    }
                  }
                }))
              }
            }
          }
          else {
            currentResponseItem.workflowProcess.tracing.push({
              ...data,
              status: WorkflowRunningStatus.Running,
            } as any)

            handleUpdateChatList(produce(chatListRef.current, (draft) => {
              const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
              if (currentIndex !== -1) {
                draft[currentIndex] = {
                  ...draft[currentIndex],
                  ...currentResponseItem,
                }
              }
            }))
          }
        },
        onNodeFinished: ({ data }: any, messageId?: string) => {
          if (!messageId)
            return

          const currentResponseItem = responseMapRef.current.get(messageId)
          if (!currentResponseItem || !currentResponseItem.workflowProcess || !currentResponseItem.workflowProcess.tracing)
            return

          if (isInIteration) {
            const tracing = currentResponseItem.workflowProcess.tracing
            if (tracing.length > 0) {
              const iterations = tracing[tracing.length - 1]
              if (iterations.details && iterations.details.length > 0) {
                const currIteration = iterations.details[iterations.details.length - 1]
                if (currIteration.length > 0) {
                  currIteration[currIteration.length - 1] = {
                    ...data,
                    status: WorkflowRunningStatus.Succeeded,
                  } as any

                  handleUpdateChatList(produce(chatListRef.current, (draft) => {
                    const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
                    if (currentIndex !== -1) {
                      draft[currentIndex] = {
                        ...draft[currentIndex],
                        ...currentResponseItem,
                      }
                    }
                  }))
                }
              }
            }
          }
          else {
            const currentIndex = currentResponseItem.workflowProcess.tracing.findIndex(item => item.node_id === data.node_id)
            if (currentIndex >= 0) {
              const extras = currentResponseItem.workflowProcess.tracing[currentIndex].extras
                ? { extras: currentResponseItem.workflowProcess.tracing[currentIndex].extras }
                : {}

              currentResponseItem.workflowProcess.tracing[currentIndex] = {
                ...extras,
                ...data,
                status: WorkflowRunningStatus.Succeeded,
              } as any

              handleUpdateChatList(produce(chatListRef.current, (draft) => {
                const currentIndex = draft.findIndex(item => item.id === currentResponseItem.id)
                if (currentIndex !== -1) {
                  draft[currentIndex] = {
                    ...draft[currentIndex],
                    ...currentResponseItem,
                  }
                }
              }))
            }
          }
        },
        onTTSChunk: (messageId: string, audio: string) => {
          if (!audio || audio === '')
            return
          player.playAudioWithAudio(audio, true)
          AudioPlayerManager.getInstance().resetMsgId(messageId)
        },
        onTTSEnd: (messageId: string, audio: string) => {
          player.playAudioWithAudio(audio, false)
        },
      })
    return true
  }, [
    checkPromptVariables,
    config?.suggested_questions_after_answer,
    updateCurrentQA,
    t,
    notify,
    promptVariablesConfig,
    handleUpdateChatList,
    handleResponding,
    formatTime,
  ])

  const handleAnnotationEdited = useCallback((query: string, answer: string, index: number) => {
    handleUpdateChatList(chatListRef.current.map((item, i) => {
      if (i === index - 1) {
        return {
          ...item,
          content: query,
        }
      }
      if (i === index) {
        return {
          ...item,
          content: answer,
          annotation: {
            ...item.annotation,
            logAnnotation: undefined,
          } as any,
        }
      }
      return item
    }))
  }, [handleUpdateChatList])
  const handleAnnotationAdded = useCallback((annotationId: string, authorName: string, query: string, answer: string, index: number) => {
    handleUpdateChatList(chatListRef.current.map((item, i) => {
      if (i === index - 1) {
        return {
          ...item,
          content: query,
        }
      }
      if (i === index) {
        const answerItem = {
          ...item,
          content: item.content,
          annotation: {
            id: annotationId,
            authorName,
            logAnnotation: {
              content: answer,
              account: {
                id: '',
                name: authorName,
                email: '',
              },
            },
          } as Annotation,
        }
        return answerItem
      }
      return item
    }))
  }, [handleUpdateChatList])
  const handleAnnotationRemoved = useCallback((index: number) => {
    handleUpdateChatList(chatListRef.current.map((item, i) => {
      if (i === index) {
        return {
          ...item,
          content: item.content,
          annotation: {
            ...(item.annotation || {}),
            id: '',
          } as Annotation,
        }
      }
      return item
    }))
  }, [handleUpdateChatList])

  return {
    chatList,
    setChatList,
    conversationId: connversationId.current,
    isResponding,
    setIsResponding,
    handleSend,
    suggestedQuestions,
    handleRestart,
    handleStop,
    handleAnnotationEdited,
    handleAnnotationAdded,
    handleAnnotationRemoved,
  }
}
