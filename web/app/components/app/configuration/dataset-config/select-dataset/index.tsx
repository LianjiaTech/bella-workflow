'use client'
import type { FC } from 'react'
import React, { useRef, useState } from 'react'
import { useGetState, useInfiniteScroll } from 'ahooks'
import cn from 'classnames'
import { useTranslation } from 'react-i18next'
import produce from 'immer'
import TypeIcon from '../type-icon'
import s from './style.module.css'
import Modal from '@/app/components/base/modal'
import type { DataSet } from '@/models/datasets'
import Button from '@/app/components/base/button'
import Loading from '@/app/components/base/loading'
import { fetchDatasets } from '@/service/datasets'

export type ISelectDataSetProps = {
  isShow: boolean
  onClose: () => void
  selectedIds: string[]
  onSelect: (dataSet: DataSet[]) => void
}

const SelectDataSet: FC<ISelectDataSetProps> = ({
  isShow,
  onClose,
  selectedIds,
  onSelect,
}) => {
  const { t } = useTranslation()
  const [selected, setSelected] = React.useState<DataSet[]>(selectedIds.map(id => ({ id }) as any))
  const [loaded, setLoaded] = React.useState(false)
  const [datasets, setDataSets] = React.useState<DataSet[] | null>(null)
  const hasNoData = !datasets || datasets?.length === 0
  const canSelectMulti = true

  const listRef = useRef<HTMLDivElement>(null)
  const [page, setPage, getPage] = useGetState(1)
  const [isNoMore, setIsNoMore] = useState(false)

  useInfiniteScroll(
    async () => {
      if (!isNoMore) {
        /**
         * {
          data: [
            {
              id: 'f11d58cc-3d61-4208-aecf-08a7a8be3145',
              name: '\u65ED\u65E5\u795E\u5DDE-\u5317\u4EAC-\u9AD8\u7EA7\u524D\u7AEF-\u5F20\u9E4F\u65FA(2...',
              description: 'useful for when you want to answer queries about the \u65ED\u65E5\u795E\u5DDE-\u5317\u4EAC-\u9AD8\u7EA7\u524D\u7AEF-\u5F20\u9E4F\u65FA(2).pdf',
              provider: 'vendor',
              permission: 'only_me',
              data_source_type: 'upload_file',
              indexing_technique: 'high_quality',
              app_count: 0,
              document_count: 1,
              word_count: 3151,
              created_by: '3d350d98-f3f8-4e9d-b4e5-64d960510352',
              created_at: 1718784384,
              updated_by: null,
              updated_at: 1718784384,
              embedding_model: 'text-embedding-3-large',
              embedding_model_provider: 'openai',
              embedding_available: true,
              retrieval_model_dict: {
                search_method: 'semantic_search',
                reranking_enable: false,
                reranking_model: {
                  reranking_provider_name: null,
                  reranking_model_name: null,
                },
                top_k: 3,
                score_threshold_enabled: false,
                score_threshold: 0.5,
              },
              tags: [

              ],
            },
            {
              id: 'ebc3d2da-4db5-4899-9d68-6eb3148879bc',
              name: '\u65ED\u65E5\u795E\u5DDE-\u5317\u4EAC-\u524D\u7AEF-\u8D75\u96F7\u6D9B.pdf...',
              description: 'useful for when you want to answer queries about the \u65ED\u65E5\u795E\u5DDE-\u5317\u4EAC-\u524D\u7AEF-\u8D75\u96F7\u6D9B.pdf',
              provider: 'vendor',
              permission: 'only_me',
              data_source_type: 'upload_file',
              indexing_technique: 'high_quality',
              app_count: 0,
              document_count: 1,
              word_count: 3264,
              created_by: '3d350d98-f3f8-4e9d-b4e5-64d960510352',
              created_at: 1718781498,
              updated_by: null,
              updated_at: 1718781498,
              embedding_model: 'text-embedding-3-large',
              embedding_model_provider: 'openai',
              embedding_available: true,
              retrieval_model_dict: {
                search_method: 'semantic_search',
                reranking_enable: false,
                reranking_model: {
                  reranking_provider_name: null,
                  reranking_model_name: null,
                },
                top_k: 3,
                score_threshold_enabled: false,
                score_threshold: 0.5,
              },
              tags: [

              ],
            },
          ],
          has_more: false,
          limit: 20,
          total: 2,
          page: 1,
        }
         */
        const { data, has_more } = await fetchDatasets({ url: '/datasets', params: { page } })
        setPage(getPage() + 1)
        setIsNoMore(!has_more)
        const newList = [...(datasets || []), ...data]
        setDataSets(newList)
        setLoaded(true)
        if (!selected.find(item => !item.name))
          return { list: [] }

        const newSelected = produce(selected, (draft) => {
          selected.forEach((item, index) => {
            if (!item.name) { // not fetched database
              const newItem = newList.find(i => i.id === item.id)
              if (newItem)
                draft[index] = newItem
            }
          })
        })
        setSelected(newSelected)
      }
      return { list: [] }
    },
    {
      target: listRef,
      isNoMore: () => {
        return isNoMore
      },
      reloadDeps: [isNoMore],
    },
  )

  const toggleSelect = (dataSet: DataSet) => {
    const isSelected = selected.some(item => item.id === dataSet.id)
    if (isSelected) {
      setSelected(selected.filter(item => item.id !== dataSet.id))
    }
    else {
      if (canSelectMulti)
        setSelected([...selected, dataSet])
      else
        setSelected([dataSet])
    }
  }

  const handleSelect = () => {
    onSelect(selected)
  }
  return (
    <Modal
      isShow={isShow}
      onClose={onClose}
      className='w-[400px]'
      wrapperClassName='!z-[101]'
      title={t('appDebug.feature.dataSet.selectTitle')}
    >
      {!loaded && (
        <div className='flex h-[200px]'>
          <Loading type='area' />
        </div>
      )}

      {(loaded && hasNoData) && (
        <div className='flex items-center justify-center mt-6 rounded-lg space-x-1  h-[128px] text-[13px] border'
          style={{
            background: 'rgba(0, 0, 0, 0.02)',
            borderColor: 'rgba(0, 0, 0, 0.02',
          }}
        >
          <span className='text-gray-500'>{t('appDebug.feature.dataSet.noDataSet')}</span>
          {/* <Link href="/datasets/create" className='font-normal text-[#155EEF]'>{t('appDebug.feature.dataSet.toCreate')}</Link> */}
        </div>
      )}

      {datasets && datasets?.length > 0 && (
        <>
          <div ref={listRef} className='mt-7 space-y-1 max-h-[286px] overflow-y-auto'>
            {datasets.map(item => (
              <div
                key={item.id}
                className={cn(s.item, selected.some(i => i.id === item.id) && s.selected, 'flex justify-between items-center h-10 px-2 rounded-lg bg-white border border-gray-200  cursor-pointer', !item.embedding_available && s.disabled)}
                onClick={() => {
                  if (!item.embedding_available)
                    return
                  toggleSelect(item)
                }}
              >
                <div className='mr-1 flex items-center'>
                  <div className={cn('mr-2', !item.embedding_available && 'opacity-50')}>
                    <TypeIcon type="upload_file" size='md' />
                  </div>
                  <div className={cn('max-w-[200px] text-[13px] font-medium text-gray-800 overflow-hidden text-ellipsis whitespace-nowrap', !item.embedding_available && 'opacity-50 !max-w-[120px]')}>{item.name}</div>
                  {!item.embedding_available && (
                    <span className='ml-1 shrink-0 px-1 border boder-gray-200 rounded-md text-gray-500 text-xs font-normal leading-[18px]'>{t('dataset.unavailable')}</span>
                  )}
                </div>

                {/* <div className={cn('shrink-0 flex text-xs text-gray-500 overflow-hidden whitespace-nowrap', !item.embedding_available && 'opacity-50')}>
                  <span className='max-w-[100px] overflow-hidden text-ellipsis whitespace-nowrap'>{formatNumber(item.word_count)}</span>
                  {t('appDebug.feature.dataSet.words')}
                  <span className='px-0.5'>·</span>
                  <span className='max-w-[100px] min-w-[8px] overflow-hidden text-ellipsis whitespace-nowrap'>{formatNumber(item.document_count)} </span>
                  {t('appDebug.feature.dataSet.textBlocks')}
                </div> */}
              </div>
            ))}
          </div>
        </>
      )}
      {loaded && (
        <div className='flex justify-between items-center mt-8'>
          <div className='text-sm  font-medium text-gray-700'>
            {selected.length > 0 && `${selected.length} ${t('appDebug.feature.dataSet.selected')}`}
          </div>
          <div className='flex space-x-2'>
            <Button className='!w-24 !h-9' onClick={onClose}>{t('common.operation.cancel')}</Button>
            <Button className='!w-24 !h-9' type='primary' onClick={handleSelect} disabled={hasNoData}>{t('common.operation.add')}</Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
export default React.memo(SelectDataSet)
