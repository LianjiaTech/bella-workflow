import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'
import { isTenantName, resolveTenantId } from '@/utils/tenant-routes'

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname

  // 处理租户名称重定向到租户ID
  const tenantMatch = pathname.match(/^\/tenant\/([^\/]+)(.*)$/)
  if (tenantMatch) {
    const [, tenantNameOrId, rest] = tenantMatch
    if (isTenantName(tenantNameOrId)) {
      const realTenantId = resolveTenantId(tenantNameOrId)
      const newPath = `/tenant/${realTenantId}${rest || ''}`
      return NextResponse.redirect(new URL(newPath, request.url))
    }
  }

  // 处理旧路由重定向到新的租户路由
  if (pathname === '/apps')
    return NextResponse.redirect(new URL('/tenant/test/apps', request.url))

  if (pathname.startsWith('/app/')) {
    const appMatch = pathname.match(/^\/app\/([^\/]+)(.*)$/)
    if (appMatch) {
      const [, appId, subPath] = appMatch
      const page = subPath.replace('/', '') || 'workflow'
      return NextResponse.redirect(new URL(`/tenant/test/app/${appId}/${page}`, request.url))
    }
  }

  if (pathname === '/datasets')
    return NextResponse.redirect(new URL('/tenant/test/datasets', request.url))

  if (pathname.startsWith('/datasets/')) {
    const datasetMatch = pathname.match(/^\/datasets\/(.*)$/)
    if (datasetMatch) {
      const [, rest] = datasetMatch
      return NextResponse.redirect(new URL(`/tenant/test/datasets/${rest}`, request.url))
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    '/apps',
    '/app/:path*',
    '/datasets',
    '/datasets/:path*',
    '/tenant/:path*',
  ],
}
