export type TenantConfig = {
  brand: string // 用于document.title 示例：bella
  displayName: string // 用于页面标题 示例：贝拉
  tenantId: string // 租户id
  appConfig: AppConfig
}

export type AppConfig = {
  appHeader?: AppHeaderConfig // 可选；无该模块，默认无应用头
  appSidebar?: boolean // 可选；是否展示侧边栏；默认不展示
  features: AppFeatures
}

export type AppHeaderConfig = {
  iconConfig?: IconConfig
  showWorkspaceProvider?: boolean // 是否展示工作空间按钮 默认值true
  showWorkroomButton?: boolean // 是否展示工作室按钮 默认值true
  showDataSourceButton?: boolean // 是否展示数据源按钮 默认值true
  showUserInfo?: boolean // 是否展示用户信息 默认值为true
}

export type IconConfig = {
  clickable?: boolean // 是否支持点击跳转事件，当前默认跳转到/apps 默认值false
}

export type AppFeatures = {
  workflow: WorkflowModule
  // 其他可选模块：配置存在即启用；不存在则不展示侧边栏入口，且访问对应路由会被前置重定向到 /404
  develop?: 'default' | {} // 开发/接口模块：控制“API 接入”入口及 /:tenantId/:appId/develop 访问
  customApi?: 'default' | {} // 自定义 API 模块：控制“自定义 API”入口及 /:tenantId/:appId/customApi 访问
  logs?: 'default' | {} // 日志模块：控制“日志”入口及 /:tenantId/:appId/logs 访问
  trigger?: 'default' | {} // 触发器模块：仅在 workflow 模式下生效；控制“触发器”入口及 /:tenantId/:appId/trigger 访问
}

export type WorkflowModule = {
  initialization: { // 目前主要用于处理bella 初始化时需要弹框填充基础进行，没有配置就不弹框
    showTitleDescModal: boolean // 默认false，根据配置的值在觉得是否弹框
    postMessage?: {
      allowedOrigins: string[] // 白名单，必须显式声明，只配置线上即可
    }
    autoPublish?: boolean // 默认false
  }
  features?: {
    header?: WorkflowHeaderConfig
  }
}

export type WorkflowHeaderConfig = {
  buttons?: WorkflowHeaderButton[] // 不配置默认全部
  hasPermission?: boolean // 画布头部权限控制
}

export type WorkflowHeaderButton = {
  id: WorkflowHeaderButtonId | OldWorkflowHeaderButtonId
  label?: string // Bella 和画像 存在只有名字不一致，其他都一致的按钮，目前用两代码份文件区分
  labels?: {
    [state: string]: string
  }
}

export type WorkflowHeaderButtonId =
  | 'runAndHistory'
  | 'customExportDsl'
  | 'customImportDsl'
  | 'copilot'
  | 'env'
  | 'publish'

export type OldWorkflowHeaderButtonId = 'exportDSL' | 'importDSL'

export const TENANT_CONFIGS: Record<string, TenantConfig> = {
}

export class TenantConfigCenter {
  static getConfig(tenantId: string): TenantConfig {
    return TENANT_CONFIGS[tenantId] || null
  }
}
