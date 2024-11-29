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

export const setTenantId = (tenantId: string) => {
  globalThis.sessionStorage?.setItem('currentTenantId', tenantId)
}

export const getTenantId = (): string => {
  return globalThis.sessionStorage?.getItem('currentTenantId') || ''
}

export const setSpaceCode = (spaceCode: string) => {
  globalThis.sessionStorage?.setItem('currentSpaceCode', spaceCode)
}

export const getSpaceCode = (userCode: string): string => {
  return globalThis.sessionStorage?.getItem('currentSpaceCode') || userCode
}

export const getUserInfo = (): { userName: string; ucid: string; tenantId: string ; spaceCode: string } => {
  const tenantId = globalThis.sessionStorage?.getItem('currentTenantId')
  const spaceCode = globalThis.sessionStorage?.getItem('currentSpaceCode')
  const userInfoStr = globalThis.localStorage?.getItem(tenantId)
  return userInfoStr != null
    ? JSON.parse(userInfoStr)
    : {
      userName: '',
      ucid: '',
      tenantId,
      spaceCode,
    }
}
