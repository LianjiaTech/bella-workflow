export const getRedirection = (
  isCurrentWorkspaceManager: boolean,
  app: any,
  redirectionFunc: (href: string) => void,
) => {
  if (!isCurrentWorkspaceManager) {
    redirectionFunc(`/app/${app.id}/overview`)
  }
  else {
    if (app.mode === 'workflow' || app.mode === 'advanced-chat')
      redirectionFunc(`/app/${app.id}/workflow?workflowName=${app?.name}&userName=${app?.userName}&ucid=${app?.ucid}`)
    else
      redirectionFunc(`/app/${app.id}/configuration`)
  }
}
