/**
 * Application Configuration Initialization File - Example
 *
 * This file demonstrates how to register custom tenant configurations.
 * In the open-source version, you can use this file as a template to create your own tenant configurations.
 * If you don't need custom tenants, you can completely remove this file.
 */
import { appConfigs } from '@/app/context/app-registry'
import Header from '@/app/components/header' // Using default header component as an example

/**
 * Initialize application configurations
 * Register your custom tenant configurations here
 */
export function initializeAppConfigs() {
  // Example tenant configuration
  appConfigs.set('/example-tenant', {
    tenantId: 'example-tenant-id',
    HeaderComponent: Header, // Can be replaced with a custom header component
    showHeader: true,
  })

  // Another example tenant that doesn't display a header
  appConfigs.set('/example-headless', {
    tenantId: 'example-headless-id',
    showHeader: false,
  })

  /**
   * Add your custom tenant configurations
   * For example:
   * appConfigs.set('/your-tenant', {
   *   tenantId: 'your-tenant-id',
   *   HeaderComponent: YourCustomHeader,
   *   showHeader: true,
   * })
   */
}

// Execute configuration initialization
initializeAppConfigs()
