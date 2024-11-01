/**
 * 获取url后的参数
 * @param param
 * @returns
 */
export const getQueryParams = (param: string): string | null => {
  const urlParams = new URLSearchParams(window.location.search)
  return urlParams.get(param)
}

export const setUserInfo = (ucid: string, userName: string, tenantId: string, spaceCode: string) => {
  globalThis.sessionStorage.setItem('currentTenantId', tenantId)
  if (userName !== '' && ucid !== '' && tenantId !== '' && spaceCode !== '') {
    const userInfo: any = {
      ucid,
      userName,
      tenantId,
      spaceCode,
    }
    globalThis.localStorage?.setItem(tenantId, JSON.stringify(userInfo))
  }
}

export const getUserInfo = (): { userName: string; ucid: string; tenantId: string ; spaceCode: string } => {
  const tenantId: string = globalThis.sessionStorage.getItem('currentTenantId')
  const userInfoStr: string = globalThis.localStorage?.getItem(tenantId)
  return userInfoStr != null
    ? JSON.parse(userInfoStr)
    : {
      userName: '',
      ucid: '',
      tenantId: '',
      spaceCode: '',
    }
}
