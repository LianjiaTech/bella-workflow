// 接收到消息类型，receiveMessage
export enum ReceiveMessageType {
  init = 'init', // 初始化画布
  rollback = 'rollback', // 刷新画布，业务逻辑为回滚
  viewHistory = 'viewHistory', // 设置为只读，并展示历史版本，业务逻辑为查看历史
}

export enum PostMessageType {
  init = 'init', // 初始化画布
  updateTime = 'updateTime', // 画布更新
  published = 'published', // 画布发布
}

export enum HeaderButtonType {
  runAndHistory = 'runAndHistory', // 运行和历史
  copilot = 'copilot', // 智能助手
  customExportDsl = 'customExportDsl', // 导出DSL
  customImportDsl = 'customImportDsl', // 导入DSL
  env = 'env',
  publish = 'publish',
}
