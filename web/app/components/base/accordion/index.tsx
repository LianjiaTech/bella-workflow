'use client'

import {
  useEffect,
  useMemo,
  useState,
} from 'react'
import {
  RiArrowRightSLine,
} from '@remixicon/react'
import { useTranslation } from 'react-i18next'
import cn from '@/utils/classnames'
import { CheckCircle } from '@/app/components/base/icons/src/vender/solid/general'

  type WorkflowProcessProps = {
    expand?: boolean
    children: any
    title?: string
  }
const Accordion = ({
  expand = false,
  children,
  title = '',
}: WorkflowProcessProps) => {
  const { t } = useTranslation()
  const [collapse, setCollapse] = useState(!expand)

  const background = useMemo(() => {
    return 'linear-gradient(180deg, #E1E4EA 0%, #EAECF0 100%)'
  }, [collapse])

  useEffect(() => {
    setCollapse(!expand)
  }, [expand])

  return (
    <div
      className={cn(
        'mb-2 rounded-xl border-[0.5px] border-black/8 w-full px-3',
        collapse ? 'py-[7px]' : 'py-2',
        collapse && 'bg-white',
      )}
      style={{
        background,
      }}
    >
      <div
        className={cn(
          'flex items-center h-[18px] cursor-pointer',
        )}
        onClick={() => setCollapse(!collapse)}
      >
        {
          <CheckCircle className='shrink-0 mr-1 w-3 h-3 text-[#12B76A]' />
        }
        <div className='grow text-xs font-medium text-gray-700'>
          {title}
        </div>
        <RiArrowRightSLine className={`'ml-1 w-3 h-3 text-gray-500' ${collapse ? '' : 'rotate-90'}`} />
      </div>
      {
        !collapse && (
          <div className='mt-1.5'>
            {
              children
            }
          </div>
        )
      }
    </div>
  )
}

export default Accordion
