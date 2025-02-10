'use client'
import { useCallback } from 'react'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import { useAppContext } from '@/context/app-context'

dayjs.extend(utc)
dayjs.extend(timezone)

const useTimestamp = () => {
  const { userProfile: { timezone } } = useAppContext()

  const formatTime = useCallback((value: number, format: string) => {
    return dayjs.unix(value).tz(timezone).format(format)
  }, [timezone])

  const formatMilliseconds = useCallback((
    ms: number,
    format = 'YYYY-MM-DD HH:mm:ss',
  ) => {
    return dayjs(ms).tz(timezone).format(format)
  }, [timezone])

  return { formatTime, formatMilliseconds }
}

export default useTimestamp
