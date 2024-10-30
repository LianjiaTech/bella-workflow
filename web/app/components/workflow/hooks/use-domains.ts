import { useEffect, useState } from 'react'
import { fetchDomainList } from '@/service/common'
import type { CustomDomain } from '@/types/workflow'

export const useDomainList = (prefix: string) => {
  const [domains, setDomains] = useState([] as CustomDomain[])
  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await fetchDomainList({ url: '/workspaces/domain/list/', prefix })
        setDomains(data)
      }
      catch (error) {
        console.error('Error fetching domain list:', error)
      }
    }

    fetchData()
  }, [prefix])
  return domains
}
