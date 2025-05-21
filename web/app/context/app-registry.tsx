'use client'
import React, { createContext, useContext } from 'react'

// Application configuration interface
export type AppConfig = {
  tenantId: string
  HeaderComponent?: React.ComponentType
  showHeader: boolean
}

// Global application registry - Uses Map structure to store path prefix to configuration mapping
export const appConfigs = new Map<string, AppConfig>()

// Configuration registration function type
export type RegisterFn = (pathPrefix: string, config: AppConfig) => void

// Create context - Only exposes the registration function
const AppRegistryContext = createContext<RegisterFn>((path, config) => {
  // Default implementation directly registers to the global Map
  appConfigs.set(path, config)
})

// Hook for child components to access the registry
export const useAppRegistry = () => useContext(AppRegistryContext)

/**
 * Checks if there are any registered application configurations
 * @returns true if there are registered configurations, false otherwise
 */
export const hasRegisteredConfigs = (): boolean => {
  return appConfigs.size > 0
}

/**
 * Attempts to load the configuration file
 * This function will try to load the configuration file, but won't throw an error if the file doesn't exist
 */
export const tryLoadConfigFile = () => {
  try {
    // Dynamically import the configuration file
    // Note: In client components, this import will be bundled
    // If the file doesn't exist, an exception will be thrown and caught
    require('@/app/config/app-configs')
    console.log('Successfully loaded app-configs.ts')
    return true
  }
  catch (error) {
    // Silently fail if the file doesn't exist
    console.log('No app-configs.ts file found, using default configuration')
    return false
  }
}

// Get application configuration based on the current path
export const getAppConfig = (pathname: string, defaultConfig: AppConfig): AppConfig => {
  // Match configuration by path prefix
  for (const [prefix, config] of appConfigs.entries()) {
    if (pathname.startsWith(prefix))
      return config
  }
  // Return default configuration if no match is found
  return defaultConfig
}

// Export context Provider component
export const AppRegistryProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <AppRegistryContext.Provider value={(path, config) => appConfigs.set(path, config)}>
      {children}
    </AppRegistryContext.Provider>
  )
}
