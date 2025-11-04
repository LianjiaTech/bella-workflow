import { getTenantId, setTenantId } from './getQueryParams'

export class TenantSessionManager {
  private static readonly DEFAULT_TENANT = 'test'

  // 设置当前租户
  static setCurrentTenant(tenantId: string): void {
    setTenantId(tenantId)
  }

  // 获取当前租户
  static getCurrentTenant(): string {
    return getTenantId() || this.DEFAULT_TENANT
  }

  // 从URL路径中提取并设置租户
  static extractAndSetFromUrl(pathname: string): string {
    const tenantMatch = pathname.match(/^\/tenant\/([^\/]+)/)
    if (tenantMatch) {
      const tenantId = tenantMatch[1]
      this.setCurrentTenant(tenantId)
      return tenantId
    }
    return this.getCurrentTenant()
  }

  // 清除租户信息
  static clearTenant(): void {
    if (typeof window !== 'undefined')
      sessionStorage.removeItem(this.TENANT_KEY)
  }

  // 获取默认租户
  static getDefaultTenant(): string {
    return this.DEFAULT_TENANT
  }

  // 检查是否是默认租户
  static isDefaultTenant(tenantId?: string): boolean {
    const currentTenant = tenantId || this.getCurrentTenant()
    return currentTenant === this.DEFAULT_TENANT
  }

  // 获取租户显示名称
  static getTenantDisplayName(tenantId?: string): string {
    const currentTenant = tenantId || this.getCurrentTenant()
    // 这里可以后续扩展为从配置中获取显示名称
    return currentTenant === this.DEFAULT_TENANT ? '默认租户' : `租户 ${currentTenant}`
  }
}
