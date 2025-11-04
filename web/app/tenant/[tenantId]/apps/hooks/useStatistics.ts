import useSWR from 'swr'
import { getAppStatistics, getWorkflowDailyConversations } from '@/service/apps'

export function useAppStatistics(id: string, period: any) {
  const { data, error, isLoading } = useSWR(
    (id && period) ? { url: `/apps/${id}/statistics/daily-conversations`, params: period.query } : null,
    getAppStatistics,
    { revalidateOnFocus: false },
  )

  return {
    data,
    isLoading,
    isError: error,
  }
}

export function useWorkflowStatistics(id: string, period: any) {
  const { data, error, isLoading } = useSWR(
    (id && period) ? { url: `/apps/${id}/statistics/daily-conversations`, params: period.query } : null,
    getWorkflowDailyConversations,
    { revalidateOnFocus: false },
  )

  return {
    data,
    isLoading,
    isError: error,
  }
}
