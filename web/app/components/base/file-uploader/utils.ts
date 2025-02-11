import mimeTypes from 'mime-types'
import { FileAppearanceTypeEnum } from './types'
import type { FileEntity } from './types'
import { upload } from '@/service/base'
import { FILE_EXTS } from '@/app/components/base/prompt-editor/constants'
import { SupportUploadFileTypes } from '@/app/components/workflow/types'
import { TransferMethod } from '@/types/app'

type FileUploadParams = {
  file: File
  onProgressCallback: (progress: number) => void
  onSuccessCallback: (res: { id: string ; purpose: string ; mime_type: string ; type: string }) => void
  onErrorCallback: () => void
}
type FileUpload = (v: FileUploadParams, isPublic?: boolean, url?: string) => void
export const fileUpload: FileUpload = ({
  file,
  onProgressCallback,
  onSuccessCallback,
  onErrorCallback,
}, isPublic, url) => {
  const formData = new FormData()
  formData.append('file', file)
  const onProgress = (e: ProgressEvent) => {
    if (e.lengthComputable) {
      const percent = Math.floor(e.loaded / e.total * 100)
      onProgressCallback(percent)
    }
  }

  upload({
    xhr: new XMLHttpRequest(),
    data: formData,
    onprogress: onProgress,
  }, isPublic, url)
    .then((res: { id: string; purpose: string ; mime_type: string ; type: string }) => {
      onSuccessCallback(res)
    })
    .catch(() => {
      onErrorCallback()
    })
}

export const getFileExtension = (fileName: string, fileMimetype: string, isRemote?: boolean) => {
  let extension = ''
  if (fileMimetype)
    extension = mimeTypes.extension(fileMimetype) || ''

  if (fileName && !extension) {
    const fileNamePair = fileName.split('.')
    const fileNamePairLength = fileNamePair.length

    if (fileNamePairLength > 1)
      extension = fileNamePair[fileNamePairLength - 1]
    else
      extension = ''
  }

  if (isRemote)
    extension = ''

  return extension
}

export const getFileAppearanceType = (fileName: string, fileMimetype: string) => {
  const extension = getFileExtension(fileName, fileMimetype)

  if (extension === 'gif')
    return FileAppearanceTypeEnum.gif

  if (FILE_EXTS.image.includes(extension.toUpperCase()))
    return FileAppearanceTypeEnum.image

  if (FILE_EXTS.video.includes(extension.toUpperCase()))
    return FileAppearanceTypeEnum.video

  if (FILE_EXTS.audio.includes(extension.toUpperCase()))
    return FileAppearanceTypeEnum.audio

  if (extension === 'html')
    return FileAppearanceTypeEnum.code

  if (extension === 'pdf')
    return FileAppearanceTypeEnum.pdf

  if (extension === 'md' || extension === 'markdown' || extension === 'mdx')
    return FileAppearanceTypeEnum.markdown

  if (extension === 'xlsx' || extension === 'xls')
    return FileAppearanceTypeEnum.excel

  if (extension === 'docx' || extension === 'doc')
    return FileAppearanceTypeEnum.word

  if (extension === 'pptx' || extension === 'ppt')
    return FileAppearanceTypeEnum.ppt

  if (FILE_EXTS.document.includes(extension.toUpperCase()))
    return FileAppearanceTypeEnum.document

  return FileAppearanceTypeEnum.custom
}

export const getSupportFileType = (fileName: string, fileMimetype: string, isCustom?: boolean) => {
  if (isCustom)
    return SupportUploadFileTypes.custom

  const extension = getFileExtension(fileName, fileMimetype)
  for (const key in FILE_EXTS) {
    if ((FILE_EXTS[key]).includes(extension.toUpperCase()))
      return key
  }

  return ''
}

export const getFileNameFromUrl = (url: string) => {
  const urlParts = url.split('/')
  return urlParts[urlParts.length - 1] || ''
}

export const getSupportFileExtensionList = (allowFileTypes: string[], allowFileExtensions: string[]) => {
  if (allowFileTypes.includes(SupportUploadFileTypes.custom))
    return allowFileExtensions.map(item => item.slice(1).toUpperCase())

  return allowFileTypes.map(type => FILE_EXTS[type]).flat()
}

export const isAllowedFileExtension = (fileName: string, fileMimetype: string, allowFileTypes: string[], allowFileExtensions: string[]) => {
  return true
}

export const fileIsUploaded = (file: FileEntity) => {
  // Check if file has server id (meaning it's successfully uploaded)
  if (file.id)
    return true
  // For local files, check if upload is complete
  if (file.transferMethod === TransferMethod.local_file)
    return file.progress === 100

  return false
}

export const downloadFile = (url: string, filename: string) => {
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.style.display = 'none'
  anchor.target = '_blank'
  anchor.title = filename
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}
