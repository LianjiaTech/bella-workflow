/**
 * 获取url后的参数
 * @param param
 * @returns
 */
export const getQueryParams = (param: string): string | null => {
  const urlParams = new URLSearchParams(window.location.search)
  return urlParams.get(param)
}

export const setUserInfo = (ucid: string, userName: string, tenantId: string) => {
  if (userName !== '' && ucid !== '' && tenantId !== '') {
    globalThis.localStorage?.setItem('ucid', ucid)
    globalThis.localStorage?.setItem('userName', userName)
    globalThis.localStorage?.setItem('tenantId', tenantId)
  }
}

export const getUserInfo = (): { userName: string; ucid: string; tenantId: string } => {
  return {
    userName: globalThis.localStorage?.getItem('userName') || '',
    ucid: globalThis.localStorage?.getItem('ucid') || '',
    tenantId: globalThis.localStorage?.getItem('tenantId') || '',
  }
}
