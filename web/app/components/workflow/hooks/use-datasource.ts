import useSWR from 'swr'
import { fetchDatasourceList } from '@/service/common'

export const useDatasourceList = (type: string) => {
  const { data, mutate, isLoading } = useSWR(
    `/workspaces/current/datasource/${type}`,
    fetchDatasourceList,
  )

  return {
    data: data || [],
    mutate,
    isLoading,
  }
}
