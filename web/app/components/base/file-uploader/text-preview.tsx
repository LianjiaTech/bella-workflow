import type { FC } from 'react'
import { createPortal } from 'react-dom'
import { RiCloseLine } from '@remixicon/react'
import React, { useEffect, useState } from 'react'
import { useHotkeys } from 'react-hotkeys-hook'

type TextPreviewProps = {
  url: string
  title: string
  onCancel: () => void
}

const TextPreview: FC<TextPreviewProps> = ({
  url,
  title,
  onCancel,
}) => {
  const [content, setContent] = useState<string>('')
  const [error, setError] = useState<string>('')

  useHotkeys('esc', onCancel)

  useEffect(() => {
    const fetchContent = async () => {
      try {
        const response = await fetch(url)
        const text = await response.text()
        setContent(text)
      }
      catch (err) {
        setError('Failed to load text content')
        console.error('Error loading text:', err)
      }
    }
    fetchContent()
  }, [url])

  return createPortal(
    <div
      className='fixed inset-0 p-8 flex items-center justify-center bg-black/80 z-[1000]'
      onClick={onCancel}
    >
      <div
        className='bg-white rounded-lg p-6 max-w-3xl max-h-[80vh] w-full overflow-auto'
        onClick={e => e.stopPropagation()}
      >
        <div className='flex justify-between items-center mb-4'>
          <h3 className='text-lg font-medium text-gray-900'>{title}</h3>
          <div
            className='flex items-center justify-center w-8 h-8 bg-gray-100 rounded-lg cursor-pointer'
            onClick={onCancel}
          >
            <RiCloseLine className='w-4 h-4 text-gray-500'/>
          </div>
        </div>
        {error
          ? (
            <div className='text-red-500'>{error}</div>
          )
          : (
            <pre className='whitespace-pre-wrap font-mono text-sm text-gray-700'>
              {content}
            </pre>
          )}
      </div>
    </div>,
    document.body,
  )
}

export default TextPreview
