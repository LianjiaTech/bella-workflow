import type { FC } from 'react'
import React from 'react'
import { useTranslation } from 'react-i18next'
import Split from '../_base/components/split'
import type { ToolNodeType } from './types'
import useConfig from './use-config'
import InputVarList from './components/input-var-list'
import Button from '@/app/components/base/button'
import Field from '@/app/components/workflow/nodes/_base/components/field'
import type { NodePanelProps } from '@/app/components/workflow/types'
import Form from '@/app/components/header/account-setting/model-provider-page/model-modal/Form'
import ConfigCredential from '@/app/components/tools/setting/build-in/config-credentials'
import Loading from '@/app/components/base/loading'
import BeforeRunForm from '@/app/components/workflow/nodes/_base/components/before-run-form'
import OutputVars, { VarItem } from '@/app/components/workflow/nodes/_base/components/output-vars'
import ResultPanel from '@/app/components/workflow/run/result-panel'
import ResponseBody from '@/app/components/workflow/nodes/_base/components/output-response-body'
import RemoveEffectVarConfirm from '@/app/components/workflow/nodes/_base/components/remove-effect-var-confirm'

const i18nPrefix = 'workflow.nodes.tool'

const Panel: FC<NodePanelProps<ToolNodeType>> = ({
  id,
  data,
}) => {
  const { t } = useTranslation()

  const {
    readOnly,
    inputs,
    toolInputVarSchema,
    setInputVar,
    handleOnVarOpen,
    filterVar,
    toolSettingSchema,
    toolSettingValue,
    setToolSettingValue,
    currCollection,
    isShowAuthBtn,
    showSetAuth,
    showSetAuthModal,
    hideSetAuthModal,
    handleSaveAuth,
    isLoading,
    isShowSingleRun,
    hideSingleRun,
    singleRunForms,
    runningStatus,
    handleRun,
    handleStop,
    runResult,
    outputVar,
    handleResponseBody,
    isShowRemoveVarConfirm,
    handleRemoveVarConfirm,
    removeVarInNode,
    key,
  } = useConfig(id, data)

  if (isLoading) {
    return <div className='flex h-[200px] items-center justify-center'>
      <Loading />
    </div>
  }

  return (
    <div className='mt-2'>
      {!readOnly && isShowAuthBtn && (
        <>
          <div className='px-4 pb-3'>
            <Button
              type='primary'
              className='w-full !h-8'
              onClick={showSetAuthModal}
            >
              {t(`${i18nPrefix}.toAuthorize`)}
            </Button>
          </div>
        </>
      )}
      {!isShowAuthBtn && <>
        <div className='px-4 pb-4 space-y-4'>
          {toolInputVarSchema.length > 0 && (
            <Field
              title={t(`${i18nPrefix}.inputVars`)}
            >
              <InputVarList
                readOnly={readOnly}
                nodeId={id}
                schema={toolInputVarSchema as any}
                value={inputs.tool_parameters}
                onChange={setInputVar}
                filterVar={filterVar}
                isSupportConstantValue
                onOpen={handleOnVarOpen}
              />
            </Field>
          )}

          {toolInputVarSchema.length > 0 && toolSettingSchema.length > 0 && (
            <Split />
          )}

          <Form
            className='space-y-4'
            itemClassName='!py-0'
            fieldLabelClassName='!text-[13px] !font-semibold !text-gray-700 uppercase'
            value={toolSettingValue}
            onChange={setToolSettingValue}
            formSchemas={toolSettingSchema as any}
            isEditMode={false}
            showOnVariableMap={{}}
            validating={false}
            inputClassName='!bg-gray-50'
            readonly={readOnly}
          />
        </div>
      </>}

      {showSetAuth && (
        <ConfigCredential
          collection={currCollection!}
          onCancel={hideSetAuthModal}
          onSaved={handleSaveAuth}
          isHideRemoveBtn
        />
      )}

      <div className='px-4 pt-4 pb-2'>
        <OutputVars>
          <>
            <Field
              title="Response"
            >
              <ResponseBody
                key={key}
                nodeId={id}
                readonly={readOnly}
                payload={inputs.result}
                onChange={handleResponseBody}
                placeholder={t(`${i18nPrefix}.result.placeholder`)}
              />
            </Field>
            {outputVar
              && <VarItem
                name={outputVar.name}
                type={outputVar.type}
                description={t(`${i18nPrefix}.outputVars.result`)}
                subItems= {outputVar.subItems}
              />}
          </>
        </OutputVars>
      </div>

      {isShowSingleRun && (
        <BeforeRunForm
          nodeName={inputs.title}
          onHide={hideSingleRun}
          forms={singleRunForms}
          runningStatus={runningStatus}
          onRun={handleRun}
          onStop={handleStop}
          result={<ResultPanel {...runResult} showSteps={false} />}
        />
      )}
      <RemoveEffectVarConfirm
        isShow={isShowRemoveVarConfirm}
        onCancel={handleRemoveVarConfirm}
        onConfirm={removeVarInNode}
      />
    </div>

  )
}

export default React.memo(Panel)
