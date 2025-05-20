package com.ke.bella.workflow.auth;

import java.net.URL;
import java.util.Map;

/**
 * Interface for HTTP authentication mechanisms.
 * Implementations of this interface provide different ways to authenticate HTTP
 * requests.
 */
public interface HttpAuthenticator {

    /**
     * Returns the type of authentication.
     * This is used to match the authentication type specified in the
     * configuration.
     *
     * @return the authentication type
     */
    String getType();

    /**
     * Returns the prefix to be used in the Authorization header.
     * For example, "Basic ", "Bearer ", etc.
     *
     * @return the prefix string
     */
    default String getPrefix() {
        return "";
    };

    /**
     * Generates the authorization value without the prefix.
     * The caller is responsible for combining this value with the prefix from
     * getPrefix().
     *
     * @param apiKey       the API key
     * @param secret       the API secret
     * @param method       the HTTP method (GET, POST, etc.)
     * @param url          the URL of the request
     * @param variablePool a map of variables that can be used in the
     *                     authorization
     *
     * @return the authorization value without prefix
     */
    String generateAuthorization(String apiKey, String secret, String method, URL url, Map<String, Object> variablePool);

    /**
     * Get the default header name for this authentication type
     *
     * @return The default header name
     */
    default String getDefaultHeaderName() {
        return "Authorization";
    }
}
