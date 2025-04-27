import type { AuthConfig } from '../types'
import { APIType } from '../types'
import type { AuthField, Authenticator } from './authenticator'
import { authenticatorRegistry, defaultGetDisplayName } from './authenticator'

class BellaAuthenticator implements Authenticator {
  type = APIType.bella

  getFields(): AuthField[] {
    return []
  }

  validate(config: AuthConfig): boolean {
    return true
  }

  getDisplayName = defaultGetDisplayName
}

const bellaAuthenticator = new BellaAuthenticator()
authenticatorRegistry.register(bellaAuthenticator)
