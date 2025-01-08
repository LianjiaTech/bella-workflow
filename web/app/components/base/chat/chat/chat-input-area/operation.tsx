import {
  forwardRef,
  memo,
} from 'react'
import {
  RiSendPlane2Fill,
} from '@remixicon/react'
import type { Theme } from '../../embedded-chatbot/theme/theme-context'
import Button from '@/app/components/base/button'
import { FileUploaderInChatInput } from '@/app/components/base/file-uploader'
import type { FileUpload } from '@/app/components/base/features/types'
import cn from '@/utils/classnames'

type OperationProps = {
  fileConfig?: FileUpload
  onSend: () => void
  theme?: Theme | null
}
const Operation = forwardRef<HTMLDivElement, OperationProps>(({
  fileConfig,
  onSend,
  theme,
}, ref) => {
  return (
    <div
      className={cn(
        'shrink-0 flex items-center justify-end',
      )}
    >
      <div
        className='flex items-center pl-1'
        ref={ref}
      >
        <div className='flex items-center space-x-1'>
          {<FileUploaderInChatInput fileConfig={fileConfig as FileUpload} />}
        </div>
        <Button
          className='ml-3 px-0 w-8'
          variant='primary'
          onClick={onSend}
          style={
            theme
              ? {
                backgroundColor: theme.primaryColor,
              }
              : {}
          }
        >
          <RiSendPlane2Fill className='w-4 h-4' />
        </Button>
      </div>
    </div>
  )
})
Operation.displayName = 'Operation'

export default memo(Operation)
