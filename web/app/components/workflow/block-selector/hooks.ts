import { useTranslation } from 'react-i18next'
import { BLOCKS } from './constants'
import { TabsEnum } from './types'

export const useBlocks = () => {
  const { t } = useTranslation()

  return BLOCKS.map((block) => {
    return {
      ...block,
      title: t(`workflow.blocks.${block.type}`),
    }
  })
}

export const useTabs = () => {
  const { t } = useTranslation()

  return [
    {
      key: TabsEnum.Blocks,
      name: t('workflow.tabs.blocks'),
    },
    // 内置工具
    // {
    //   key: TabsEnum.BuiltInTool,
    //   name: t('workflow.tabs.builtInTool'),
    // },
    // 自定义工具
    {
      key: TabsEnum.CustomTool,
      name: t('workflow.tabs.customTool'),
    },
  ]
}
