'use client'

// Libraries
import { useMemo, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslation } from 'react-i18next'
import { useDebounceFn } from 'ahooks'

// Components
import Datasets from './Datasets'
import TabSliderNew from '@/app/components/base/tab-slider-new'
import SearchInput from '@/app/components/base/search-input'
import TagFilter from '@/app/components/base/tag-management/filter'
import RDBPage from '@/app/components/rdb'

// Services

// Hooks
import { useTabSearchParams } from '@/hooks/use-tab-searchparams'
import { useStore as useTagStore } from '@/app/components/base/tag-management/store'
import { useAppContext } from '@/context/app-context'

const Container = () => {
  const { t } = useTranslation()
  const router = useRouter()
  const { currentWorkspace } = useAppContext()
  const showTagManagementModal = useTagStore(s => s.showTagManagementModal)

  const options = useMemo(() => {
    return [
      { value: 'dataset', text: t('datasource.file') },
      { value: 'rdb', text: t('datasource.rdb') },
      { value: 'redis', text: t('datasource.redis') },
      { value: 'kafka', text: t('datasource.kafka') },
      { value: 'object', text: t('datasource.object') },
    ]
  }, [t])

  const [activeTab, setActiveTab] = useTabSearchParams({
    defaultTab: 'dataset',
  })
  const containerRef = useRef<HTMLDivElement>(null)

  const [keywords, setKeywords] = useState('')
  const [searchKeywords, setSearchKeywords] = useState('')
  const { run: handleSearch } = useDebounceFn(() => {
    setSearchKeywords(keywords)
  }, { wait: 500 })
  const handleKeywordsChange = (value: string) => {
    setKeywords(value)
    handleSearch()
  }
  const [tagFilterValue, setTagFilterValue] = useState<string[]>([])
  const [tagIDs, setTagIDs] = useState<string[]>([])
  const { run: handleTagsUpdate } = useDebounceFn(() => {
    setTagIDs(tagFilterValue)
  }, { wait: 500 })
  const handleTagsChange = (value: string[]) => {
    setTagFilterValue(value)
    handleTagsUpdate()
  }

  return (
    <div ref={containerRef} className='grow relative flex flex-col bg-gray-100 overflow-y-auto'>
      <div className='sticky top-0 flex justify-between pt-4 px-12 pb-2 leading-[56px] bg-gray-100 z-10 flex-wrap gap-y-2'>
        <TabSliderNew
          value={activeTab}
          onChange={newActiveTab => setActiveTab(newActiveTab)}
          options={options}
        />
        {activeTab === 'dataset' && (
          <div className='flex items-center gap-2'>
            <TagFilter type='knowledge' value={tagFilterValue} onChange={handleTagsChange} />
            <SearchInput className='w-[200px]' value={keywords} onChange={handleKeywordsChange} />
          </div>
        )}
      </div>

      {activeTab === 'dataset' && (
        <>
          <Datasets containerRef={containerRef} tags={tagIDs} keywords={searchKeywords} />
          {/* <DatasetFooter /> */}
          {/* {showTagManagementModal && (
            <TagManagementModal type='knowledge' show={showTagManagementModal} />
          )} */}
        </>
      )}

      {activeTab === 'rdb' && <RDBPage />}
    </div>

  )
}

export default Container
