'use client'
import React from 'react'
import { useTranslation } from 'react-i18next'
import type { Authenticator } from '../../auth/authenticator'
import type { AuthConfig } from '../../types'
import Field from './field'
import Input from '@/app/components/workflow/nodes/_base/components/input-support-select-var'

type AuthFieldsProps = {
  authenticator: Authenticator
  config: AuthConfig | null | undefined
  onChange: (fieldName: string, value: string) => void
  availableVars: any[]
  availableNodesWithParent: any[]
  onFocusChange: (isFocus: boolean) => void
}

const AuthFields: React.FC<AuthFieldsProps> = ({
  authenticator,
  config,
  onChange,
  availableVars,
  availableNodesWithParent,
  onFocusChange,
}) => {
  const { t } = useTranslation()
  return (
    <>
      {authenticator.getFields().map(field => (
        <Field
          key={field.name}
          title={field.displayName}
          isRequired={field.required}
        >
          <Input
            instanceId={`http-authorization-${field.name}`}
            className='w-full h-8 leading-8 px-2.5 pt-1 rounded-lg border-0 bg-gray-100 text-gray-900 text-[13px] placeholder:text-gray-400 focus:outline-none focus:ring-1 focus:ring-inset focus:ring-gray-200'
            value={config?.[field.name] || field.defaultValue || ''}
            onChange={(value: string) => onChange(field.name, value)}
            readOnly={false}
            nodesOutputVars={availableVars}
            availableNodes={availableNodesWithParent}
            onFocusChange={onFocusChange}
            placeholder={field.description || t('workflow.nodes.http.insertVarPlaceholder') || ''}
            promptMinHeightClassName='h-full'
          />
        </Field>
      ))}
    </>
  )
}

export default AuthFields
