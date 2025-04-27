'use client'
import produce from 'immer'
import type { FC } from 'react'
import React, { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { AuthConfig, Authorization as AuthorizationPayloadType } from '../../types'
import { APIType, AuthorizationType } from '../../types'
import { authenticatorRegistry } from '../../auth'
import RadioGroup from './radio-group'
import AuthFields from './auth-fields'
import { VarType } from '@/app/components/workflow/types'
import type { Var } from '@/app/components/workflow/types'
import useAvailableVarList from '@/app/components/workflow/nodes/_base/hooks/use-available-var-list'
import Modal from '@/app/components/base/modal'
import Button from '@/app/components/base/button'

const i18nPrefix = 'workflow.nodes.http.authorization'

type Props = {
  nodeId: string
  payload: AuthorizationPayloadType
  onChange: (payload: AuthorizationPayloadType) => void
  isShow: boolean
  onHide: () => void
}

const Field = ({ title, isRequired, children }: { title: string; isRequired?: boolean; children: JSX.Element }) => {
  return (
    <div>
      <div className='leading-8 text-[13px] font-medium text-gray-700'>
        {title}
        {isRequired && <span className='ml-0.5 text-[#D92D20]'>*</span>}
      </div>
      <div>{children}</div>
    </div>
  )
}

const Authorization: FC<Props> = ({
  nodeId,
  payload,
  onChange,
  isShow,
  onHide,
}) => {
  const { t } = useTranslation()

  const [isFocus, setIsFocus] = useState(false)
  const { availableVars, availableNodesWithParent } = useAvailableVarList(nodeId, {
    onlyLeafNodeVar: false,
    filterVar: (varPayload: Var) => {
      return [VarType.string, VarType.number, VarType.secret].includes(varPayload.type)
    },
  })

  const [tempPayload, setTempPayload] = React.useState<AuthorizationPayloadType>(payload)

  const authenticators = authenticatorRegistry.getAllAuthenticators()

  const currentAuthenticator = tempPayload.config?.type
    ? authenticatorRegistry.getAuthenticator(tempPayload.config.type)
    : undefined

  const handleAuthTypeChange = useCallback((type: string) => {
    const newPayload = produce(tempPayload, (draft: AuthorizationPayloadType) => {
      draft.type = type as AuthorizationType
      if (draft.type === AuthorizationType.apiKey && !draft.config) {
        const firstAuthenticator = authenticators[0]
        if (firstAuthenticator) {
          draft.config = {
            type: firstAuthenticator.type as APIType,
            api_key: '',
          }
        }
        else {
          draft.config = {
            type: APIType.basic,
            api_key: '',
          }
        }
      }
    })
    setTempPayload(newPayload)
  }, [tempPayload, authenticators])

  const handleAuthAPITypeChange = useCallback((type: string) => {
    const newPayload = produce(tempPayload, (draft: AuthorizationPayloadType) => {
      if (!draft.config) {
        draft.config = {
          type: type as APIType,
          api_key: '',
        }
      }
      else {
        const oldValues = { ...draft.config }

        draft.config = {
          type: type as APIType,
          api_key: '',
        }

        if (oldValues.type === type) {
          Object.keys(oldValues).forEach((key) => {
            if (key !== 'type' && key in oldValues)
              draft.config![key as keyof AuthConfig] = oldValues[key as keyof typeof oldValues]
          })
        }
      }
    })
    setTempPayload(newPayload)
  }, [tempPayload])

  const handleFieldChange = useCallback((fieldName: string) => {
    return (value: string) => {
      const newPayload = produce(tempPayload, (draft: AuthorizationPayloadType) => {
        if (!draft.config) {
          const firstAuthenticator = authenticators[0]
          if (firstAuthenticator) {
            draft.config = {
              type: firstAuthenticator.type as APIType,
              api_key: '',
            }
          }
          else {
            draft.config = {
              type: APIType.basic,
              api_key: '',
            }
          }
        }

        draft.config![fieldName as keyof AuthConfig] = value
      })
      setTempPayload(newPayload)
    }
  }, [tempPayload, authenticators])

  const handleConfirm = useCallback(() => {
    onChange(tempPayload)
    onHide()
  }, [tempPayload, onChange, onHide])

  const authTypeOptions = authenticators.map(authenticator => ({
    value: authenticator.type,
    label: authenticator.getDisplayName(t),
  }))

  return (
    <Modal
      title={t(`${i18nPrefix}.authorization`)}
      isShow={isShow}
      onClose={onHide}
    >
      <div>
        <div className='space-y-2'>
          <Field title={t(`${i18nPrefix}.authorizationType`)}>
            <RadioGroup
              options={[
                { value: AuthorizationType.none, label: t(`${i18nPrefix}.no-auth`) },
                { value: AuthorizationType.apiKey, label: t(`${i18nPrefix}.api-key`) },
              ]}
              value={tempPayload.type}
              onChange={handleAuthTypeChange}
            />
          </Field>

          {tempPayload.type === AuthorizationType.apiKey && (
            <>
              <Field title={t(`${i18nPrefix}.auth-type`)}>
                <RadioGroup
                  options={authTypeOptions.length > 0
                    ? authTypeOptions
                    : Object.values(APIType).map((type) => {
                      return { value: type, label: type }
                    })
                  }
                  value={tempPayload.config?.type || APIType.basic}
                  onChange={handleAuthAPITypeChange}
                />
              </Field>

              {currentAuthenticator && (
                <AuthFields
                  authenticator={currentAuthenticator}
                  config={tempPayload.config}
                  onChange={(fieldName, value) => handleFieldChange(fieldName)(value)}
                  availableVars={availableVars}
                  availableNodesWithParent={availableNodesWithParent}
                  onFocusChange={setIsFocus}
                />
              )}
            </>
          )}
        </div>
        <div className='mt-6 flex justify-end space-x-2'>
          <Button onClick={onHide}>{t('common.operation.cancel')}</Button>
          <Button variant='primary' onClick={handleConfirm}>{t('common.operation.save')}</Button>
        </div>
      </div>
    </Modal>
  )
}
export default React.memo(Authorization)
