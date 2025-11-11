import { getAppRoute } from './tenant-routes'

export const getRedirection = (
  isCurrentWorkspaceEditor: boolean,
  app: any,
  redirectionFunc: (href: string) => void,
  tenantId?: string,
) => {
  if (!isCurrentWorkspaceEditor) {
    redirectionFunc(getAppRoute(app.id, 'overview', tenantId))
  }
  else {
    if (app.mode === 'workflow' || app.mode === 'advanced-chat')
      redirectionFunc(getAppRoute(app.id, 'workflow', tenantId))
    else
      redirectionFunc(getAppRoute(app.id, 'configuration', tenantId))
  }
}
