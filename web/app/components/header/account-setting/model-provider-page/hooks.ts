import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'
import useSWR, { useSWRConfig } from 'swr'
import { useContext } from 'use-context-selector'
import type {
  CustomConfigrationModelFixedFields,
  DefaultModel,
  DefaultModelResponse,
  Model,
  ModelTypeEnum,
} from './declarations'
import {
  ConfigurateMethodEnum,
  ModelStatusEnum,
} from './declarations'
import I18n from '@/context/i18n'
import {
  fetchModelProviderCredentials,
  getPayUrl,
  submitFreeQuota,
} from '@/service/common'
import { useProviderContext } from '@/context/provider-context'

type UseDefaultModelAndModelList = (
  defaultModel: DefaultModelResponse | undefined,
  modelList: Model[],
) => [DefaultModel | undefined, (model: DefaultModel) => void]
export const useSystemDefaultModelAndModelList: UseDefaultModelAndModelList = (
  defaultModel,
  modelList,
) => {
  const currentDefaultModel = useMemo(() => {
    const currentProvider = modelList.find(provider => provider.provider === defaultModel?.provider.provider)
    const currentModel = currentProvider?.models.find(model => model.model === defaultModel?.model)
    const currentDefaultModel = currentProvider && currentModel && {
      model: currentModel.model,
      provider: currentProvider.provider,
    }

    return currentDefaultModel
  }, [defaultModel, modelList])
  const [defaultModelState, setDefaultModelState] = useState<DefaultModel | undefined>(currentDefaultModel)
  const handleDefaultModelChange = useCallback((model: DefaultModel) => {
    setDefaultModelState(model)
  }, [])
  useEffect(() => {
    setDefaultModelState(currentDefaultModel)
  }, [currentDefaultModel])

  return [defaultModelState, handleDefaultModelChange]
}

export const useLanguage = () => {
  const { locale } = useContext(I18n)
  return locale.replace('-', '_')
}

export const useProviderCrenditialsFormSchemasValue = (
  provider: string,
  configurateMethod: ConfigurateMethodEnum,
  configured?: boolean,
  currentCustomConfigrationModelFixedFields?: CustomConfigrationModelFixedFields,
) => {
  const { data: predefinedFormSchemasValue } = useSWR(
    (configurateMethod === ConfigurateMethodEnum.predefinedModel && configured)
      ? `/workspaces/current/model-providers/${provider}/credentials`
      : null,
    fetchModelProviderCredentials,
  )
  const { data: customFormSchemasValue } = useSWR(
    (configurateMethod === ConfigurateMethodEnum.customizableModel && currentCustomConfigrationModelFixedFields)
      ? `/workspaces/current/model-providers/${provider}/models/credentials?model=${currentCustomConfigrationModelFixedFields?.__model_name}&model_type=${currentCustomConfigrationModelFixedFields?.__model_type}`
      : null,
    fetchModelProviderCredentials,
  )

  const value = useMemo(() => {
    return configurateMethod === ConfigurateMethodEnum.predefinedModel
      ? predefinedFormSchemasValue?.credentials
      : customFormSchemasValue?.credentials
        ? {
          ...customFormSchemasValue?.credentials,
          ...currentCustomConfigrationModelFixedFields,
        }
        : undefined
  }, [
    configurateMethod,
    currentCustomConfigrationModelFixedFields,
    customFormSchemasValue?.credentials,
    predefinedFormSchemasValue?.credentials,
  ])

  return value
}

export const useModelList = (type: ModelTypeEnum) => {
  const { data, mutate, isLoading } = {
    data: [],
    mutate: () => {},
    isLoading: false,
  }// useSWR(`/workspaces/current/models/model-types/${type}`, fetchModelList)

  return {
    data: [
      {
        provider: 'openai',
        label: {
          zh_Hans: 'OpenAI',
          en_US: 'OpenAI',
        },
        icon_small: {
          zh_Hans: 'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_small/zh_Hans',
          en_US: 'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_small/en_US',
        },
        icon_large: {
          zh_Hans: 'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_large/zh_Hans',
          en_US: 'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_large/en_US',
        },
        status: 'active',
        models: [
          {
            model: 'c4ai-command-r-plus',
            label: {
              zh_Hans: 'c4ai-command-r-plus',
              en_US: 'c4ai-command-r-plus',
            },
            model_type: 'llm',
            features: [
              'multi-tool-call',
              'agent-thought',
              'stream-tool-call',
            ],
            fetch_from: 'predefined-model',
            model_properties: {
              mode: 'chat',
              context_size: 16385,
            },
            deprecated: false,
            status: 'active',
            load_balancing_enabled: false,
          },
        ],
      },

    ],
    mutate,
    isLoading,
  }
}

export const useDefaultModel = (type: ModelTypeEnum) => {
  const { data, mutate, isLoading } = { data: null }
  // useSWR(`/workspaces/current/default-model?model_type=${type}`, fetchDefaultModal)

  return {
    data: {
      model: 'c4ai-command-r-plus',
      model_type: 'llm',
      provider: {
        provider: 'openai',
        label: {
          zh_Hans: 'OpenAI',
          en_US: 'OpenAI',
        },
        icon_small: {
          zh_Hans:
            'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_small/zh_Hans',
          en_US:
            'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_small/en_US',
        },
        icon_large: {
          zh_Hans:
            'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_large/zh_Hans',
          en_US:
            'https://cloud.dify.ai/console/api/workspaces/current/model-providers/openai/icon_large/en_US',
        },
        supported_model_types: [
          'llm',
          'text-embedding',
          'speech2text',
          'moderation',
          'tts',
        ],
        models: [],
      },
    },
    mutate,
    isLoading,
  }
}

export const useCurrentProviderAndModel = (modelList: Model[], defaultModel?: DefaultModel) => {
  const currentProvider = modelList.find(provider => provider.provider === defaultModel?.provider)
  const currentModel = currentProvider?.models.find(model => model.model === defaultModel?.model)

  return {
    currentProvider,
    currentModel,
  }
}

export const useTextGenerationCurrentProviderAndModelAndModelList = (defaultModel?: DefaultModel) => {
  const { textGenerationModelList } = useProviderContext()
  const activeTextGenerationModelList = textGenerationModelList.filter(model => model.status === ModelStatusEnum.active)
  const {
    currentProvider,
    currentModel,
  } = useCurrentProviderAndModel(textGenerationModelList, defaultModel)

  return {
    currentProvider,
    currentModel,
    textGenerationModelList,
    activeTextGenerationModelList,
  }
}

export const useModelListAndDefaultModel = (type: ModelTypeEnum) => {
  const { data: modelList } = useModelList(type)
  const { data: defaultModel } = useDefaultModel(type)

  return {
    modelList,
    defaultModel,
  }
}

export const useModelListAndDefaultModelAndCurrentProviderAndModel = (type: ModelTypeEnum) => {
  const { modelList, defaultModel } = useModelListAndDefaultModel(type)
  const { currentProvider, currentModel } = useCurrentProviderAndModel(
    modelList,
    { provider: defaultModel?.provider.provider || '', model: defaultModel?.model || '' },
  )

  return {
    modelList,
    defaultModel,
    currentProvider,
    currentModel,
  }
}

export const useUpdateModelList = () => {
  const { mutate } = useSWRConfig()

  const updateModelList = useCallback((type: ModelTypeEnum) => {
    mutate(`/workspaces/current/models/model-types/${type}`)
  }, [mutate])

  return updateModelList
}

export const useAnthropicBuyQuota = () => {
  const [loading, setLoading] = useState(false)

  const handleGetPayUrl = async () => {
    if (loading)
      return

    setLoading(true)
    try {
      const res = await getPayUrl('/workspaces/current/model-providers/anthropic/checkout-url')

      window.location.href = res.url
    }
    finally {
      setLoading(false)
    }
  }

  return handleGetPayUrl
}

export const useFreeQuota = (onSuccess: () => void) => {
  const [loading, setLoading] = useState(false)

  const handleClick = async (type: string) => {
    if (loading)
      return

    try {
      setLoading(true)
      const res = await submitFreeQuota(`/workspaces/current/model-providers/${type}/free-quota-submit`)

      if (res.type === 'redirect' && res.redirect_url)
        window.location.href = res.redirect_url
      else if (res.type === 'submit' && res.result === 'success')
        onSuccess()
    }
    finally {
      setLoading(false)
    }
  }

  return handleClick
}

export const useModelProviders = () => {
  const { data: providersData, mutate, isLoading } = {
    data: [],
    mutate: () => {},
    isLoading: false,
  }
  // useSWR('/workspaces/current/model-providers', fetchModelProviders)

  return {
    data: providersData?.data || [],
    mutate,
    isLoading,
  }
}

export const useUpdateModelProviders = () => {
  const { mutate } = useSWRConfig()

  const updateModelProviders = useCallback(() => {
    mutate('/workspaces/current/model-providers')
  }, [mutate])

  return updateModelProviders
}
