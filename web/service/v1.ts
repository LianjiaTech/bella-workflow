import { get } from './base'
import type { DraftInfo } from '@/types/v1'

// export const fetchTagList = (type: string) => {
//   return get<Tag[]>('/tags', { params: { type } })
// }

export const fetchDraftInfo = (workflowId: string, page: number, limit: number) => {
  return get<DraftInfo>(`/apps/${workflowId}/workflows`, { params: { page, limit } })
}

// export const updateTag = (tagID: string, name: string) => {
//   return patch(`/tags/${tagID}`, {
//     body: {
//       name,
//     },
//   })
// }

// export const deleteTag = (tagID: string) => {
//   return del(`/tags/${tagID}`)
// }

// export const bindTag = (tagIDList: string[], targetID: string, type: string) => {
//   return post('/tag-bindings/create', {
//     body: {
//       tag_ids: tagIDList,
//       target_id: targetID,
//       type,
//     },
//   })
// }

// export const unBindTag = (tagID: string, targetID: string, type: string) => {
//   return post('/tag-bindings/remove', {
//     body: {
//       tag_id: tagID,
//       target_id: targetID,
//       type,
//     },
//   })
// }
