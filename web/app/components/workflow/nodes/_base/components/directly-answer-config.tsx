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
  generateDeltaContent: boolean
  onChange: (memory?: boolean) => void
}

const DirectlyAnswerConfig: FC<Props> = ({
  className,
  readonly,
  generateDeltaContent,
  onChange,
}) => {
  const { t } = useTranslation()
  const handleEnabledChange = useCallback((enabled: boolean) => {
    onChange(enabled)
  }, [onChange])

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
      >
      </Field>
    </div>
  )
}
export default React.memo(DirectlyAnswerConfig)
