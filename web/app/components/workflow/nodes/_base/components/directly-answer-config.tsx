'use client'
import type { FC } from 'react'
import React, { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import cn from 'classnames'
import Field from '@/app/components/workflow/nodes/_base/components/field'
import Switch from '@/app/components/base/switch'

const i18nPrefix = 'workflow.nodes.common.delta'

type Props = {
  className?: string
  readonly: boolean
  generateDeltaContent?: boolean
  generateNewMessage?: boolean
  onChange: (memory: boolean) => void
  onNewMessageChange: (newMessage: boolean) => void
}

const DirectlyAnswerConfig: FC<Props> = ({
  className,
  readonly,
  generateDeltaContent,
  generateNewMessage,
  onChange,
  onNewMessageChange,
}) => {
  const { t } = useTranslation()
  const handleEnabledChange = useCallback((enabled: boolean) => {
    onChange(enabled)
  }, [onChange])

  const handleNewMessageChange = useCallback((enabled: boolean) => {
    onNewMessageChange(enabled)
  }, [onNewMessageChange])

  return (
    <div className={cn(className)}>
      <Field
        title={t(`${i18nPrefix}.title`)}
        tooltip={t(`${i18nPrefix}.tip`)!}
        operations={
          <Switch
            defaultValue={generateDeltaContent}
            onChange={handleEnabledChange}
            size='md'
            disabled={readonly}
          />
        }
      />
      {/* <Field
        title={t(`${i18nPrefix}.new_msg`)}
        tooltip={t(`${i18nPrefix}.new_msg_tip`)!}
        operations={
          <Switch
            defaultValue={generateNewMessage}
            onChange={handleNewMessageChange}
            size='md'
            disabled={readonly}
          />
        }
      /> */}
    </div>
  )
}
export default React.memo(DirectlyAnswerConfig)
