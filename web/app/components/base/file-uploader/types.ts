import type { TransferMethod } from '@/types/app'

export enum FileAppearanceTypeEnum {
  image = 'image',
  video = 'video',
  audio = 'audio',
  document = 'document',
  code = 'code',
  pdf = 'pdf',
  markdown = 'markdown',
  excel = 'excel',
  word = 'word',
  ppt = 'ppt',
  gif = 'gif',
  custom = 'custom',
}

export type FileAppearanceType = keyof typeof FileAppearanceTypeEnum

export type FileEntity = {
  /* id 是前端临时主键, id是后端生成的file_id */
  id?: string
  filename: string
  size: number
  type: string
  mime_type?: string
  progress: number
  transferMethod: TransferMethod
  supportFileType: string
  originalFile?: File
  base64Url?: string
  url?: string
  isRemote?: boolean
  purpose?: string
  _id: string
}
