'use client'
import React from 'react'

type FieldProps = {
  title: string
  isRequired?: boolean
  children: React.ReactNode
}

const Field: React.FC<FieldProps> = ({ title, isRequired, children }) => {
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

export default Field
