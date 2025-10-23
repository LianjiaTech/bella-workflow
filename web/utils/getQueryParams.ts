/**
 * 获取url后的参数
 * @param param
 * @returns
 */
export const getQueryParams = (param: string): string | null => {
  const urlParams = new URLSearchParams(window?.location.search)
  return urlParams?.get(param)
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
  if (tenantId)
    globalThis.sessionStorage?.setItem('currentTenantId', tenantId)
}

export const getTenantId = (): string => {
  return globalThis.sessionStorage?.getItem('currentTenantId') || ''
}

export const setSpaceCode = (spaceCode: string) => {
  if (spaceCode)
    globalThis.sessionStorage?.setItem('currentSpaceCode', spaceCode)
}

export const getSpaceCode = (userCode: string): string => {
  return globalThis.sessionStorage?.getItem('currentSpaceCode') || userCode
}

export const getUserInfo = (): { userName: string; ucid: string; tenantId: string ; spaceCode: string } => {
  let tenantId = globalThis.sessionStorage?.getItem('currentTenantId') || ''
  const spaceCode = globalThis.sessionStorage?.getItem('currentSpaceCode') || ''

  // Fallback on first load before Layout sets sessionStorage
  if (!tenantId && typeof globalThis.location !== 'undefined') {
    const pathname = globalThis.location.pathname || ''
    const firstSeg = pathname.split('/')?.[1] || ''
    if (firstSeg === 'app' || firstSeg === 'apps')
      tenantId = 'test'
    else if (firstSeg)
      tenantId = firstSeg
    else
      tenantId = getQueryParams('tenantId') || 'test'

    try {
      globalThis.sessionStorage?.setItem('currentTenantId', tenantId)
    }
    catch (e) {
      // ignore
    }
  }

  const userInfoStr = tenantId ? globalThis.localStorage?.getItem(tenantId) : null
  return userInfoStr != null
    ? JSON.parse(userInfoStr)
    : {
      userName: '',
      ucid: '',
      tenantId,
      spaceCode,
    }
}
