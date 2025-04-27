package com.ke.bella.workflow.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating HTTP authenticators.
 * This class uses Spring dependency injection to discover and load
 * authenticator implementations.
 * To add a new authenticator:
 * 1. Implement the HttpAuthenticator interface
 * 2. Add @Component annotation to your implementation
 */
@Component
public class HttpAuthenticatorFactory {

    private static final Logger LOGGER = Logger.getLogger(HttpAuthenticatorFactory.class.getName());
    private static final Map<String, HttpAuthenticator> authenticators = new HashMap<>();
    private static boolean initialized = false;
    private static HttpAuthenticatorFactory instance;

    /**
     * Constructor for Spring dependency injection.
     *
     * @param authenticatorList List of HttpAuthenticator beans managed by
     *                          Spring
     */
    @Autowired(required = false)
    public HttpAuthenticatorFactory(List<HttpAuthenticator> authenticatorList) {
        // Store the instance for static method access
        instance = this;

        // Register all Spring-managed authenticators
        if(authenticatorList != null && !authenticatorList.isEmpty()) {
            for (HttpAuthenticator authenticator : authenticatorList) {
                register(authenticator);
                LOGGER.info("Registered Spring-managed authenticator: " + authenticator.getClass().getName() +
                        " for type: " + authenticator.getType());
            }
            initialized = true;
        } else {
            LOGGER.warning("No Spring-managed authenticators found. Authentication may not work properly.");
        }
    }

    /**
     * Register a new authenticator.
     *
     * @param authenticator The authenticator to register
     */
    public static void register(HttpAuthenticator authenticator) {
        if(authenticator == null) {
            LOGGER.warning("Attempted to register null authenticator");
            return;
        }

        String type = authenticator.getType();
        if(type == null || type.isEmpty()) {
            LOGGER.warning("Authenticator " + authenticator.getClass().getName() +
                    " has null or empty type and will not be registered");
            return;
        }

        String lowerType = type.toLowerCase();
        HttpAuthenticator existing = authenticators.get(lowerType);
        if(existing != null) {
            LOGGER.info("Replacing authenticator for type '" + lowerType + "': " +
                    existing.getClass().getName() + " -> " + authenticator.getClass().getName());
        }

        authenticators.put(lowerType, authenticator);
    }

    /**
     * Get an authenticator by type.
     *
     * @param type The authenticator type
     *
     * @return The authenticator
     *
     * @throws IllegalArgumentException if no authenticator is found for the
     *                                  given type
     */
    public static HttpAuthenticator getAuthenticator(String type) {
        if(!initialized) {
            LOGGER.warning("HttpAuthenticatorFactory not initialized. Make sure Spring context is properly set up.");
            if(instance == null) {
                throw new IllegalStateException(
                        "HttpAuthenticatorFactory not initialized by Spring. Make sure it is properly configured in your Spring context.");
            }
        }

        if(type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Authentication type cannot be empty");
        }

        String lowerType = type.toLowerCase();
        HttpAuthenticator authenticator = authenticators.get(lowerType);

        if(authenticator == null) {
            throw new IllegalArgumentException("No authenticator implementation found for type '" + type + "'");
        }

        return authenticator;
    }
}
