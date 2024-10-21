import type { FC } from 'react'
import React, {
  memo,
  useCallback,
} from 'react'
import { useTranslation } from 'react-i18next'
import VarReferencePicker from '../_base/components/variable/var-reference-picker'
import useConfig from './use-config'
import RetrievalConfig from './components/retrieval-config'
import AddKnowledge from './components/add-dataset'
import DatasetList from './components/dataset-list'
import type { RagNodeType } from './types'
import AdvancedSetting from './components/advanced-setting'
import Field from '@/app/components/workflow/nodes/_base/components/field'
import Split from '@/app/components/workflow/nodes/_base/components/split'
import OutputVars, { VarItem } from '@/app/components/workflow/nodes/_base/components/output-vars'
import { InputVarType, type NodePanelProps } from '@/app/components/workflow/types'
import BeforeRunForm from '@/app/components/workflow/nodes/_base/components/before-run-form'
import ResultPanel from '@/app/components/workflow/run/result-panel'
import ModelParameterModal from '@/app/components/header/account-setting/model-provider-page/model-parameter-modal'
import DirectlyAnswerConfig from '@/app/components/workflow/nodes/_base/components/directly-answer-config'

const i18nPrefix = 'workflow.nodes.rag'

const Panel: FC<NodePanelProps<RagNodeType>> = ({
  id,
  data,
}) => {
  const { t } = useTranslation()

  const {
    readOnly,
    inputs,
    isChatMode,
    ragModelIsChatModel,
    hasSetBlockStatus,
    handleInstructionChange,
    handleQueryVarChange,
    availableVars,
    availableNodesWithParent,
    filterVar,
    handleModelChanged,
    handleRagGenerationModelChanged,
    handleRagCompletionParamsChange,
    handleCompletionParamsChange,
    handleRetrievalModeChange,
    handleMultipleRetrievalConfigChange,
    handleDeltaChange,
    handleNewMessageChange,
    selectedDatasets,
    handleOnDatasetsChange,
    isShowSingleRun,
    hideSingleRun,
    runningStatus,
    handleRun,
    handleStop,
    query,
    setQuery,
    runResult,
    rerankModelOpen,
    setRerankModelOpen,
  } = useConfig(id, data)
  const model = inputs.generation_config?.model
  const handleOpenFromPropsChange = useCallback((openFromProps: boolean) => {
    setRerankModelOpen(openFromProps)
  }, [setRerankModelOpen])

  return (
    <div className='mt-2'>
      <div className='px-4 pb-4 space-y-4'>
        {/* {JSON.stringify(inputs, null, 2)} */}
        <Field
          title={t(`${i18nPrefix}.queryVariable`)}
        >
          <VarReferencePicker
            nodeId={id}
            readonly={readOnly}
            isShowNodeName
            value={inputs.query_variable_selector}
            onChange={handleQueryVarChange}
            filterVar={filterVar}
          />
        </Field>
        <Field
          title={t(`${i18nPrefix}.generationModel`)}
        >
          <ModelParameterModal
            popupClassName='!w-[387px]'
            isInWorkflow
            isAdvancedMode={true}
            mode={model?.mode as string}
            provider={model?.provider as string}
            completionParams={model?.completion_params as any}
            modelId={model?.name as string}
            setModel={handleRagGenerationModelChanged}
            onCompletionParamsChange={handleRagCompletionParamsChange}
            hideDebugWithMultipleModel
            debugWithMultipleModel={false}
            readonly={readOnly}
          />
        </Field>
        <Field
          title={t(`${i18nPrefix}.knowledge`)}
          operations={
            <div className='flex items-center space-x-1'>
              <RetrievalConfig
                payload={{
                  retrieval_mode: inputs.retrieval_mode,
                  multiple_retrieval_config: inputs.multiple_retrieval_config,
                  single_retrieval_config: inputs.single_retrieval_config,
                }}
                onRetrievalModeChange={handleRetrievalModeChange}
                onMultipleRetrievalConfigChange={handleMultipleRetrievalConfigChange}
                singleRetrievalModelConfig={inputs.single_retrieval_config?.model}
                onSingleRetrievalModelChange={handleModelChanged as any}
                onSingleRetrievalModelParamsChange={handleCompletionParamsChange}
                readonly={readOnly || !selectedDatasets.length}
                openFromProps={rerankModelOpen}
                onOpenFromPropsChange={handleOpenFromPropsChange}
                selectedDatasets={selectedDatasets}
              />
              {!readOnly && (<div className='w-px h-3 bg-gray-200'></div>)}
              {!readOnly && (
                <AddKnowledge
                  selectedIds={inputs.dataset_ids}
                  onChange={handleOnDatasetsChange}
                />
              )}
            </div>
          }
        >
          <DatasetList
            list={selectedDatasets}
            onChange={handleOnDatasetsChange}
            readonly={readOnly}
          />
        </Field>
        {isChatMode && (
          <>
            <DirectlyAnswerConfig
              readonly={readOnly}
              generateDeltaContent={inputs.generateDeltaContent || false}
              generateNewMessage={inputs.generateNewMessage}
              onChange={handleDeltaChange}
              onNewMessageChange={handleNewMessageChange}
            />
          </>
        )}
        <Field
          title={t(`${i18nPrefix}.advancedSetting`)}
          supportFold
        >
          <AdvancedSetting
            hideMemorySetting={true}
            instruction={inputs.generation_config?.instruction as string}
            onInstructionChange={handleInstructionChange}
            memory={undefined}
            onMemoryChange={useCallback(() => {
            }, [])}
            readonly={readOnly}
            isChatApp={isChatMode}
            isChatModel={ragModelIsChatModel}
            hasSetBlockStatus={hasSetBlockStatus}
            nodesOutputVars={availableVars}
            availableNodes={availableNodesWithParent}
          />
        </Field>
      </div>

      <Split/>
      <div className='px-4 pt-4 pb-2'>
        <OutputVars>
          <>
            <VarItem
              name='content'
              type='Array[Object]'
              description={t(`${i18nPrefix}.outputVars.contents.info`)}
              subItems={[
                {
                  name: 'type',
                  type: 'string',
                  description: t(`${i18nPrefix}.outputVars.contents.type`),
                },
                // url, title, link like bing search reference result: link, link page title, link page icon
                {
                  name: 'text',
                  type: 'string',
                  description: t(`${i18nPrefix}.outputVars.contents.text.info`),
                  subItems: [
                    {
                      name: 'value',
                      type: 'string',
                      description: t(`${i18nPrefix}.outputVars.contents.text.value`),
                    },
                    {
                      name: 'annotations',
                      type: 'Array[Object]',
                      description: t(`${i18nPrefix}.outputVars.contents.text.annotations`),
                      subItems: [],
                    },
                  ],
                },
                {
                  name: 'image_url',
                  type: 'object',
                  description: t(`${i18nPrefix}.outputVars.contents.image_url.info`),
                  subItems: [
                    {
                      name: 'url',
                      type: 'string',
                      description: t(`${i18nPrefix}.outputVars.contents.image_url.url`),
                    },
                    {
                      name: 'detail',
                      type: 'string',
                      description: t(`${i18nPrefix}.outputVars.contents.image_url.detail`),
                    },
                  ],
                },
                {
                  name: 'image_file',
                  type: 'object',
                  description: t(`${i18nPrefix}.outputVars.contents.image_file.info`),
                  subItems: [
                    {
                      name: 'file_id',
                      type: 'string',
                      description: t(`${i18nPrefix}.outputVars.contents.image_file.file_id`),
                    },
                    {
                      name: 'detail',
                      type: 'string',
                      description: t(`${i18nPrefix}.outputVars.contents.image_file.detail`),
                    },
                  ],
                },
              ]}
            />

          </>
        </OutputVars>
        {isShowSingleRun && (
          <BeforeRunForm
            nodeName={inputs.title}
            onHide={hideSingleRun}
            forms={[
              {
                inputs: [{
                  label: t(`${i18nPrefix}.queryVariable`)!,
                  variable: 'query',
                  type: InputVarType.paragraph,
                  required: true,
                  alias: Array.isArray(inputs.query_variable_selector) ? `#${inputs.query_variable_selector?.join('.')}#` : '',
                }],
                values: { query },
                onChange: keyValue => setQuery((keyValue as any).query),
              },
            ]}
            runningStatus={runningStatus}
            onRun={handleRun}
            onStop={handleStop}
            result={<ResultPanel {...runResult} showSteps={false}/>}
          />
        )}
      </div>
    </div>
  )
}

export default memo(Panel)
