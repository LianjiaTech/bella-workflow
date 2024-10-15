package com.ke.bella.workflow.service.code;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.RandomStringUtils;

import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.node.HttpNode;
import com.ke.bella.workflow.node.HttpNode.Data;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;
import com.ke.bella.workflow.utils.KeIAM;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;

public class Requests {

    static final String METHOD = "method";

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        okhttp3.Response res;
        String text;

        public int code() {
            return res.code();
        }

        public String text() throws IOException {
            readBody();
            return text;
        }

        public Object json() throws IOException {
            return JsonUtils.fromJson(text(), Object.class);
        }

        private synchronized void readBody() throws IOException {
            ResponseBody body = res.body();
            if(text == null) {
                text = body.string();
            }
        }
    }

    public Response post(Map<String, Object> options) throws IOException {
        options.put(METHOD, "POST");
        return request(options);
    }

    public Response get(Map<String, Object> options) throws IOException {
        options.put(METHOD, "GET");
        return request(options);
    }

    public Response put(Map<String, Object> options) throws IOException {
        options.put(METHOD, "PUT");
        return request(options);
    }

    public Response head(Map<String, Object> options) throws IOException {
        options.put(METHOD, "HEAD");
        return request(options);
    }

    public Response delete(Map<String, Object> options) throws IOException {
        options.put(METHOD, "DELETE");
        return request(options);
    }

    public Response option(Map<String, Object> options) throws IOException {
        options.put(METHOD, "OPTION");
        return request(options);
    }

    @SuppressWarnings("unchecked")
    public Response request(Map<String, Object> options) throws IOException {
        String method = (String) options.getOrDefault(METHOD, "GET");
        String url = (String) options.get("url");
        Map<String, Object> params = (Map<String, Object>) options.get("params");
        Object data = options.get("data");
        Object json = options.get("json");
        Map<String, String> headers = (Map<String, String>) options.get("headers");
        Map<String, String> cookies = (Map<String, String>) options.get("cookies");
        Map<String, String> auth = (Map<String, String>) options.get("auth");
        Object timeout = options.get("timeout");
        boolean allowRedirects = (boolean) options.getOrDefault("allow_redirects", true);
        Map<String, String> proxies = (Map<String, String>) options.get("proxies");
        Object verify = options.get("verify");
        Object cert = options.get("cert");

        // Build URL with query parameters
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if(params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        // Build request
        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());

        // Add headers
        if(headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        // Add cookies
        if(cookies != null) {
            StringBuilder cookieHeader = new StringBuilder();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                if(cookieHeader.length() > 0)
                    cookieHeader.append("; ");
                cookieHeader.append(entry.getKey()).append("=").append(entry.getValue());
            }
            requestBuilder.addHeader("Cookie", cookieHeader.toString());
        }

        // Add Auth header
        if(auth != null) {
            HttpNode.Data.Authorization.Config authConfig = JsonUtils.convertValue(auth, HttpNode.Data.Authorization.Config.class);
            if(authConfig != null) {
                requestBuilder.addHeader(authConfig.header(), authValue(authConfig, urlBuilder.build().url(), method));
            }
        }

        // Build request body
        RequestBody requestBody = buildRequestBody(method, data, json);
        requestBuilder.method(method, requestBody);

        OkHttpClient.Builder clientBuilder = HttpUtils.clientBuilder();

        // Set timeout
        if(timeout instanceof Number) {
            long timeoutMillis = ((Number) timeout).longValue() * 1000;
            clientBuilder.connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
        } else if(timeout instanceof List) {
            List<Number> timeoutList = (List<Number>) timeout;
            if(timeoutList.size() >= 2) {
                clientBuilder.connectTimeout(timeoutList.get(0).longValue() * 1000, TimeUnit.MILLISECONDS)
                        .readTimeout(timeoutList.get(1).longValue() * 1000, TimeUnit.MILLISECONDS);
            }
        }

        // Set redirect policy
        if(!allowRedirects) {
            clientBuilder.followRedirects(false).followSslRedirects(false);
        }

        // Set proxy
        if(proxies != null && !proxies.isEmpty()) {
            String proxyUrl = proxies.values().iterator().next();
            clientBuilder.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress(proxyUrl, 8080)));
        }

        // Set SSL verification
        if(verify instanceof Boolean && !((Boolean) verify).booleanValue()) {
            clientBuilder.sslSocketFactory(createInsecureSSLSocketFactory(), createTrustAllManager());
            clientBuilder.hostnameVerifier((hostname, session) -> true);
        } else if(verify instanceof String) {
            setCustomTrustManager(clientBuilder, (String) verify);
        }

        // Set client certificate
        if(cert != null) {
            if(cert instanceof String) {
                setClientCertificate(clientBuilder, (String) cert, null);
            } else if(cert instanceof List) {
                List<String> certList = (List<String>) cert;
                if(certList.size() >= 2) {
                    setClientCertificate(clientBuilder, certList.get(0), certList.get(1));
                }
            }
        }

        OkHttpClient client = clientBuilder.build();
        okhttp3.Response res = client.newCall(requestBuilder.build()).execute();
        return Response.builder().res(res).build();
    }

    public static String authValue(Data.Authorization.Config config, URL url, String method) {
        String apiKey = "";
        String authType = config.getType();
        if("bella".equals(authType)) {
            apiKey = BellaContext.getApiKey();
        } else if("ke-IAM".equalsIgnoreCase(authType)) {
            apiKey = KeIAM.generateAuthorization(config.getApiKey(), config.getSecret(),
                    RandomStringUtils.randomNumeric(9), method.toUpperCase(), url.getPath(), url.getHost(), url.getQuery());
        } else {
            apiKey = config.getApiKey();
        }
        return config.prefix() + apiKey;
    }

    @SuppressWarnings("unchecked")
    private static RequestBody buildRequestBody(String method, Object data, Object json) {
        if(HttpMethod.permitsRequestBody(method)) {
            if(json != null) {
                String jsonBody = JsonUtils.toJson(json);
                return RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
            } else if(data != null) {
                if(data instanceof Map) {
                    FormBody.Builder formBodyBuilder = new FormBody.Builder();
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) data).entrySet()) {
                        formBodyBuilder.add(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                    return formBodyBuilder.build();
                } else if(data instanceof String) {
                    return RequestBody.create((String) data, MediaType.parse("text/plain; charset=utf-8"));
                }
            }
        }
        return null;
    }

    private static SSLSocketFactory createInsecureSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { createTrustAllManager() }, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static X509TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static void setCustomTrustManager(OkHttpClient.Builder clientBuilder, String caBundlePath) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            int index = 0;
            try (java.io.InputStream is = new java.io.FileInputStream(caBundlePath)) {
                for (X509Certificate cert : (java.util.Collection<X509Certificate>) certificateFactory.generateCertificates(is)) {
                    String alias = Integer.toString(index++);
                    keyStore.setCertificateEntry(alias, cert);
                }
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagerFactory.getTrustManagers()[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setClientCertificate(OkHttpClient.Builder clientBuilder, String certPath, String keyPath) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new java.io.FileInputStream(certPath), null);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, null);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), createTrustAllManager());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
