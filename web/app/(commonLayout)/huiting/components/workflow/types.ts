// 接收到消息类型，receiveMessage
export enum ReceiveMessageType {
  rollback = 'rollback', // 刷新画布，业务逻辑为回滚
  viewHistory = 'viewHistory', // 设置为只读，并展示历史版本，业务逻辑为查看历史
}

export enum PostMessageType {
  updateTime = 'updateTime', // 画布更新
  published = 'published', // 画布发布
}
