import type { APIType, AuthConfig } from '../types'

export type AuthField = {
  name: string
  displayName: string
  description?: string
  required: boolean
  defaultValue?: string
}

export type Authenticator = {
  type: APIType | string

  getFields(): AuthField[]

  validate(config: AuthConfig): boolean

  getDisplayName(t: (key: string, defaultValue?: string) => string): string
}

export function defaultGetDisplayName(this: Authenticator, t: (key: string, defaultValue?: string) => string): string {
  return t(`workflow.nodes.http.authorization.${this.type}`)
}

class AuthenticatorRegistry {
  private authenticators: Map<string, Authenticator> = new Map()

  register(authenticator: Authenticator): void {
    if (!authenticator.getDisplayName)
      (authenticator as any).getDisplayName = defaultGetDisplayName.bind(authenticator)

    this.authenticators.set(authenticator.type, authenticator)
  }

  getAuthenticator(type: string): Authenticator | undefined {
    return this.authenticators.get(type)
  }

  getAllAuthenticators(): Authenticator[] {
    return Array.from(this.authenticators.values())
  }
}

export const authenticatorRegistry = new AuthenticatorRegistry()
