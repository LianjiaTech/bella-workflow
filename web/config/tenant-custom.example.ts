import type { TenantConfig } from './tenant'

// 自定义租户配置（私有化部署时使用）
// 开源版本中，这个文件可以为空或包含示例配置
export const CUSTOM_TENANT_CONFIGS: Record<string, TenantConfig> = {
  // 示例配置 - 可以在私有化部署时替换为实际租户配置
  // 'example-tenant-id': {
  //   brand: 'example',
  //   displayName: '示例租户',
  //   tenantId: 'example-tenant-id',
  //   appConfig: {
  //     appHeader: {
  //       showWorkspaceProvider: true,
  //       showWorkroomButton: true,
  //       showDataSourceButton: true,
  //       showUserInfo: true,
  //     },
  //     appSidebar: false,
  //     features: {
  //       workflow: {
  //         initialization: {
  //           showTitleDescModal: false,
  //         },
  //       },
  //       develop: 'default',
  //       customApi: 'default',
  //       logs: 'default',
  //       trigger: 'default',
  //     },
  //   },
  // },
}

// 租户名称到ID的映射（私有化部署时使用）
export const CUSTOM_TENANT_NAME_MAPPINGS: Record<string, string> = {
  // 示例映射 - 可以在私有化部署时替换为实际映射
  // 'example': 'example-tenant-id',
}
