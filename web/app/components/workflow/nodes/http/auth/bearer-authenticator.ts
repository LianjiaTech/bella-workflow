import type { AuthConfig } from '../types'
import { APIType } from '../types'
import type { AuthField, Authenticator } from './authenticator'
import { authenticatorRegistry, defaultGetDisplayName } from './authenticator'

class BearerAuthenticator implements Authenticator {
  type = APIType.bearer

  getFields(): AuthField[] {
    return [
      {
        name: 'api_key',
        displayName: 'API Key',
        required: true,
      },
    ]
  }

  validate(config: AuthConfig): boolean {
    return !!config.api_key
  }

  getDisplayName = defaultGetDisplayName
}

const bearerAuthenticator = new BearerAuthenticator()
authenticatorRegistry.register(bearerAuthenticator)
