import { TenantSessionManager } from './tenant-session'
import { TENANT_NAME_MAPPINGS } from '@/config/tenant'

// 获取租户名称到ID的映射（动态配置）
function getTenantNameMappings(): Record<string, string> {
  return TENANT_NAME_MAPPINGS
}

// 获取租户ID到名称的映射（反向映射）
function getTenantIdToNameMappings(): Record<string, string> {
  const mappings = getTenantNameMappings()
  const reverseMappings: Record<string, string> = {}
  Object.entries(mappings).forEach(([name, id]) => {
    reverseMappings[id] = name
  })
  return reverseMappings
}

/**
 * 将租户名称转换为租户ID
 * @param tenantNameOrId 租户名称或ID
 * @returns 租户ID
 */
export function resolveTenantId(tenantNameOrId: string): string {
  const mappings = getTenantNameMappings()
  return mappings[tenantNameOrId] || tenantNameOrId
}

/**
 * 将租户ID转换为租户名称（如果有映射的话）
 * @param tenantId 租户ID
 * @returns 租户名称或原ID
 */
export function resolveTenantName(tenantId: string): string {
  const mappings = getTenantIdToNameMappings()
  return mappings[tenantId] || tenantId
}

/**
 * 检查是否是租户名称（需要重定向）
 * @param tenantNameOrId 租户名称或ID
 * @returns 是否是租户名称
 */
export function isTenantName(tenantNameOrId: string): boolean {
  const mappings = getTenantNameMappings()
  return tenantNameOrId in mappings
}
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
  const resolvedTenantId = resolveTenantId(tenantId)
  return `/tenant/${resolvedTenantId}${cleanPath}`
}

/**
 * 从路径中提取租户ID
 * @param pathname 路径名
 * @returns 租户ID或null
 */
export function extractTenantIdFromPath(pathname: string): string | null {
  const match = pathname.match(/^\/tenant\/([^\/]+)/)
  if (!match)
    return null

  const tenantNameOrId = match[1]
  return resolveTenantId(tenantNameOrId)
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
  const baseRoute = `/tenant/${tenantId}/app/${appId}/${page}`

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

// === 新增的便捷函数，自动使用当前租户 ===

/**
 * 获取应用路由（自动使用当前租户）
 * @param appId 应用ID
 * @param page 页面名称
 * @param tenantId 可选的租户ID，如果不提供则使用当前租户
 * @returns 完整的应用路由
 */
export function getAppRoute(appId: string, page = 'workflow', tenantId?: string): string {
  const finalTenantId = tenantId || TenantSessionManager.getCurrentTenant()
  return generateAppRoute(finalTenantId, appId, page)
}

/**
 * 获取应用列表路由（自动使用当前租户）
 * @returns 应用列表路由
 */
export function getAppsRoute(): string {
  const tenantId = TenantSessionManager.getCurrentTenant()
  return `/tenant/${tenantId}/apps`
}

/**
 * 获取数据集路由（自动使用当前租户）
 * @param datasetId 可选的数据集ID
 * @param tenantId 可选的租户ID，如果不提供则使用当前租户
 * @returns 数据集路由
 */
export function getDatasetsRoute(datasetId?: string, tenantId?: string): string {
  const finalTenantId = tenantId || TenantSessionManager.getCurrentTenant()
  const basePath = `/tenant/${finalTenantId}/datasets`
  return datasetId ? `${basePath}/${datasetId}` : basePath
}

/**
 * 获取数据源路由（自动使用当前租户）
 * @param datasetId 可选的数据源ID
 * @param tenantId 可选的租户ID，如果不提供则使用当前租户
 * @returns 数据源路由
 */
export function getDatasourcesRoute(datasetId?: string, tenantId?: string): string {
  const finalTenantId = tenantId || TenantSessionManager.getCurrentTenant()
  const basePath = `/tenant/${finalTenantId}/datasources`
  return datasetId ? `${basePath}/${datasetId}` : basePath
}

/**
 * 获取租户根路由（自动使用当前租户）
 * @param path 子路径
 * @returns 租户路由
 */
export function getTenantRoute(path = ''): string {
  const tenantId = TenantSessionManager.getCurrentTenant()
  return generateTenantRoute(tenantId, path)
}

// === 兼容性函数：将旧路由转换为新路由 ===

/**
 * 将旧路由转换为新的租户路由
 * @param path 旧路径
 * @returns 新的租户路由
 */
export function convertLegacyRoute(path: string): string {
  // /apps -> /tenant/{currentTenant}/apps
  if (path === '/apps')
    return getAppsRoute()

  // /app/{appId} -> /tenant/{currentTenant}/app/{appId}
  const appMatch = path.match(/^\/app\/([^\/]+)(.*)$/)
  if (appMatch) {
    const [, appId, subPath] = appMatch
    const page = subPath.replace('/', '') || 'workflow'
    return getAppRoute(appId, page)
  }

  // /datasets -> /tenant/{currentTenant}/datasets
  if (path === '/datasets' || path.startsWith('/datasets/')) {
    const datasetId = path.split('/')[2]
    return getDatasetsRoute(datasetId)
  }

  return path
}

/**
 * 检查路径是否是租户路由
 * @param pathname 路径名
 * @returns 是否是租户路由
 */
export function isTenantRoute(pathname: string): boolean {
  return pathname.startsWith('/tenant/')
}

/**
 * 检查路径是否是需要转换的旧路由
 * @param pathname 路径名
 * @returns 是否是旧路由
 */
export function isLegacyRoute(pathname: string): boolean {
  return pathname === '/apps'
         || pathname.startsWith('/app/')
         || pathname === '/datasets'
         || pathname.startsWith('/datasets/')
}
