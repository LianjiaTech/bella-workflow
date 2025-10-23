/**
 * 生成租户路由
 * @param tenantId 租户ID
 * @param path 路径
 * @returns 完整的租户路由
 */
export function generateTenantRoute(
  tenantId: string,
  path = '',
): string {
  const cleanPath = path.startsWith('/') ? path : `/${path}`
  return `/${tenantId}${cleanPath}`
}

/**
 * 从路径中提取租户ID
 * @param pathname 路径名
 * @returns 租户ID或null
 */
export function extractTenantIdFromPath(pathname: string): string | null {
  const match = pathname.match(/^\/([^\/]+)/)
  return match ? match[1] : null
}

/**
 * 生成应用路由
 * @param tenantId 租户ID
 * @param appId 应用ID
 * @param page 页面名称
 * @param query 查询参数
 * @returns 完整的应用路由
 */
export function generateAppRoute(
  tenantId: string,
  appId: string,
  page = 'workflow',
  query?: Record<string, string>,
): string {
  const baseRoute = `/${tenantId}/${appId}/${page}`

  if (query && Object.keys(query).length > 0) {
    const searchParams = new URLSearchParams(query)
    return `${baseRoute}?${searchParams.toString()}`
  }

  return baseRoute
}

/**
 * 生成品牌路由（用于兼容性）
 * @param brand 品牌名称
 * @param path 路径
 * @returns 品牌路由
 */
export function generateBrandRoute(brand: string, path = ''): string {
  const cleanPath = path.startsWith('/') ? path : `/${path}`
  return `/${brand}${cleanPath}`
}
