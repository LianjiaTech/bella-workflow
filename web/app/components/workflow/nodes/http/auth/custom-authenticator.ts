import type { AuthConfig } from '../types'
import { APIType } from '../types'
import type { AuthField, Authenticator } from './authenticator'
import { authenticatorRegistry, defaultGetDisplayName } from './authenticator'

class CustomAuthenticator implements Authenticator {
  type = APIType.custom

  getFields(): AuthField[] {
    return [
      {
        name: 'header',
        displayName: 'Header',
        required: true,
        defaultValue: '',
      },
      {
        name: 'api_key',
        displayName: 'API Key',
        required: true,
      },
    ]
  }

  validate(config: AuthConfig): boolean {
    return !!config.header && !!config.api_key
  }

  getDisplayName = defaultGetDisplayName
}

const customAuthenticator = new CustomAuthenticator()
authenticatorRegistry.register(customAuthenticator)
