package com.ke.bella.workflow.service.code;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ke.bella.workflow.auth.HttpAuthenticator;
import com.ke.bella.workflow.auth.HttpAuthenticatorFactory;
import com.ke.bella.workflow.node.HttpNode;
import com.ke.bella.workflow.node.HttpNode.Data;
import com.ke.bella.workflow.utils.HttpUtils;
import com.ke.bella.workflow.utils.JsonUtils;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okio.BufferedSink;

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

    public Object post(Map<String, Object> options) throws IOException {
        options.put(METHOD, "POST");
        return request(options);
    }

    public Object get(Map<String, Object> options) throws IOException {
        options.put(METHOD, "GET");
        return request(options);
    }

    public Object put(Map<String, Object> options) throws IOException {
        options.put(METHOD, "PUT");
        return request(options);
    }

    public Object head(Map<String, Object> options) throws IOException {
        options.put(METHOD, "HEAD");
        return request(options);
    }

    public Object delete(Map<String, Object> options) throws IOException {
        options.put(METHOD, "DELETE");
        return request(options);
    }

    public Object option(Map<String, Object> options) throws IOException {
        options.put(METHOD, "OPTION");
        return request(options);
    }

    @SuppressWarnings("unchecked")
    public Object request(Map<String, Object> options) throws IOException {
        String method = (String) options.getOrDefault(METHOD, "GET");
        String url = (String) options.get("url");
        Map<String, Object> params = (Map<String, Object>) options.get("params");
        Object data = options.get("data");
        Object json = options.get("json");
        Object files = options.get("files");
        Map<String, String> headers = (Map<String, String>) options.get("headers");
        Map<String, String> cookies = (Map<String, String>) options.get("cookies");
        Map<String, String> auth = (Map<String, String>) options.get("auth");
        Object timeout = options.get("timeout");
        boolean allowRedirects = (boolean) options.getOrDefault("allow_redirects", true);
        Map<String, String> proxies = (Map<String, String>) options.get("proxies");
        Object verify = options.get("verify");
        Object cert = options.get("cert");
        boolean stream = (boolean) options.getOrDefault("stream", false);

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
        RequestBody requestBody = buildRequestBody(method, data, json, files);
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
        if(stream) {
            return streamRequest(client, requestBuilder.build());
        } else {
            okhttp3.Response res = client.newCall(requestBuilder.build()).execute();
            return Response.builder().res(res).build();
        }
    }

    public static String authValue(Data.Authorization.Config config, URL url, String method) {
        String authType = config.getType();
        HttpAuthenticator authenticator = HttpAuthenticatorFactory.getAuthenticator(authType);
        String authValue = authenticator.generateAuthorization(
                config.getApiKey(),
                config.getSecret(),
                method.toUpperCase(),
                url,
                new HashMap<>());
        return authenticator.getPrefix() + authValue;
    }

    private Iterable<Object> streamRequest(OkHttpClient client, Request request) {
        return Flowable.create(emitter -> {
            RealEventSource eventSource = new RealEventSource(request, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String rawdata) {
                    Map<String, String> data2 = new LinkedHashMap<>();
                    data2.put("id", id);
                    data2.put("type", type);
                    data2.put("data", rawdata);

                    emitter.onNext(data2);
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    emitter.onComplete();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, okhttp3.Response response) {
                    emitter.onError(t);
                }
            });
            eventSource.connect(client);
        }, BackpressureStrategy.BUFFER).blockingIterable();
    }

    @SuppressWarnings("unchecked")
    private static RequestBody buildRequestBody(String method, Object data, Object json, Object files) {
        if(!HttpMethod.permitsRequestBody(method)) {
            return null;
        }

        if(files != null) {
            return buildMultipartBody(data, files);
        }

        if(json != null) {
            String jsonBody = JsonUtils.toJson(json);
            return RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        }

        if(data != null) {
            if(data instanceof Map) {
                FormBody.Builder formBodyBuilder = new FormBody.Builder();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) data).entrySet()) {
                    Object value = entry.getValue();
                    if(value != null) {
                        formBodyBuilder.add(entry.getKey(), String.valueOf(value));
                    }
                }
                return formBodyBuilder.build();
            } else if(data instanceof String) {
                return RequestBody.create((String) data, MediaType.parse("text/plain; charset=utf-8"));
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static RequestBody buildMultipartBody(Object data, Object files) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        if(data instanceof Map) {
            ((Map<String, Object>) data).forEach((key, value) -> {
                if(value != null) {
                    builder.addFormDataPart(key, String.valueOf(value));
                }
            });
        } else if(data instanceof List) {
            for (Object entry : (List<?>) data) {
                if(entry instanceof List) {
                    List<?> tuple = (List<?>) entry;
                    if(tuple.size() >= 2 && tuple.get(0) != null && tuple.get(1) != null) {
                        builder.addFormDataPart(tuple.get(0).toString(), tuple.get(1).toString());
                    }
                } else if(entry instanceof Map) {
                    Map<?, ?> entryMap = (Map<?, ?>) entry;
                    Object key = entryMap.get("name");
                    Object value = entryMap.get("value");
                    if(key != null && value != null) {
                        builder.addFormDataPart(key.toString(), value.toString());
                    }
                }
            }
        }

        addMultipartFiles(builder, files);
        return builder.build();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addMultipartFiles(MultipartBody.Builder builder, Object files) {
        if(files instanceof Map) {
            ((Map<String, Object>) files).forEach((name, spec) -> addMultipartFile(builder, name, spec));
        } else if(files instanceof List) {
            for (Object entry : (List) files) {
                if(entry instanceof List) {
                    List tuple = (List) entry;
                    if(tuple.size() >= 2) {
                        Object name = tuple.get(0);
                        Object spec = tuple.get(1);
                        if(name != null) {
                            addMultipartFile(builder, name.toString(), spec);
                        }
                    }
                } else if(entry instanceof Map) {
                    Map mapEntry = (Map) entry;
                    Object name = mapEntry.getOrDefault("name", mapEntry.getOrDefault("field", "file"));
                    if(name != null) {
                        addMultipartFile(builder, name.toString(), mapEntry);
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addMultipartFile(MultipartBody.Builder builder, String name, Object spec) {
        if(spec == null) {
            return;
        }

        MultipartFileSpec fileSpec = MultipartFileSpec.from(name, spec);
        RequestBody body = createRequestBody(fileSpec);

        if(!fileSpec.getHeaders().isEmpty()) {
            Map<String, String> headers = new LinkedHashMap<>(fileSpec.getHeaders());
            boolean hasContentDisposition = headers.keySet().stream().anyMatch(k -> "Content-Disposition".equalsIgnoreCase(k));
            if(!hasContentDisposition) {
                headers.put("Content-Disposition", contentDisposition(name, fileSpec.getFilename()));
            }
            boolean hasContentType = headers.keySet().stream().anyMatch(k -> "Content-Type".equalsIgnoreCase(k));
            if(!hasContentType && fileSpec.getContentType() != null) {
                headers.put("Content-Type", fileSpec.getContentType());
            }
            builder.addPart(Headers.of(headers), body);
        } else {
            builder.addFormDataPart(name, fileSpec.getFilename(), body);
        }
    }

    private static RequestBody createRequestBody(MultipartFileSpec fileSpec) {
        Object content = fileSpec.getContent();

        if(content == null) {
            return RequestBody.create(new byte[0], resolveBinaryMediaType(fileSpec.getContentType(), fileSpec.getFilename(), null));
        }

        if(content instanceof File) {
            File file = (File) content;
            MediaType mediaType = resolveBinaryMediaType(fileSpec.getContentType(), fileSpec.getFilename(), file.getName());
            return RequestBody.create(file, mediaType);
        }

        if(content instanceof InputStream) {
            MediaType mediaType = resolveBinaryMediaType(fileSpec.getContentType(), fileSpec.getFilename(), null);
            return streamRequestBody((InputStream) content, mediaType);
        }

        if(content instanceof byte[]) {
            MediaType mediaType = resolveBinaryMediaType(fileSpec.getContentType(), fileSpec.getFilename(), null);
            return RequestBody.create((byte[]) content, mediaType);
        }

        if(content instanceof String) {
            MediaType mediaType = resolveTextMediaType(fileSpec.getContentType());
            return RequestBody.create((String) content, mediaType);
        }

        if(content instanceof Number || content instanceof Boolean) {
            MediaType mediaType = resolveTextMediaType(fileSpec.getContentType());
            return RequestBody.create(String.valueOf(content), mediaType);
        }

        if(content instanceof Map || content instanceof List) {
            String json = JsonUtils.toJson(content);
            MediaType mediaType = resolveJsonMediaType(fileSpec.getContentType());
            return RequestBody.create(json, mediaType);
        }

        byte[] bytes = String.valueOf(content).getBytes(StandardCharsets.UTF_8);
        MediaType mediaType = resolveBinaryMediaType(fileSpec.getContentType(), fileSpec.getFilename(), null);
        return RequestBody.create(bytes, mediaType);
    }

    private static RequestBody streamRequestBody(InputStream inputStream, MediaType mediaType) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream source = inputStream) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while((read = source.read(buffer)) != -1) {
                        sink.write(buffer, 0, read);
                    }
                }
            }
        };
    }

    private static MediaType resolveBinaryMediaType(String explicitType, String providedFilename, String fallbackFilename) {
        MediaType parsed = safeParseMediaType(explicitType);
        if(parsed != null) {
            return parsed;
        }

        String filename = providedFilename != null ? providedFilename : fallbackFilename;
        if(filename != null) {
            String guessed = URLConnection.guessContentTypeFromName(filename);
            MediaType guessedType = safeParseMediaType(guessed);
            if(guessedType != null) {
                return guessedType;
            }
        }

        return MediaType.parse("application/octet-stream");
    }

    private static MediaType resolveTextMediaType(String explicitType) {
        MediaType parsed = safeParseMediaType(explicitType);
        if(parsed != null) {
            return parsed;
        }
        return MediaType.parse("text/plain; charset=utf-8");
    }

    private static MediaType resolveJsonMediaType(String explicitType) {
        MediaType parsed = safeParseMediaType(explicitType);
        if(parsed != null) {
            return parsed;
        }
        return MediaType.parse("application/json; charset=utf-8");
    }

    private static MediaType safeParseMediaType(String value) {
        if(value == null || value.isEmpty()) {
            return null;
        }
        try {
            return MediaType.parse(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String contentDisposition(String name, String filename) {
        StringBuilder builder = new StringBuilder("form-data; name=\"").append(name).append("\"");
        if(filename != null) {
            builder.append("; filename=\"").append(filename).append("\"");
        }
        return builder.toString();
    }

    @Getter
    private static class MultipartFileSpec {
        String name;
        String filename;
        Object content;
        String contentType;
        Map<String, String> headers;

        MultipartFileSpec(String name, String filename, Object content, String contentType, Map<String, String> headers) {
            this.name = name;
            this.filename = filename;
            this.content = content;
            this.contentType = contentType;
            this.headers = headers != null ? headers : new LinkedHashMap<>();
        }

        static MultipartFileSpec from(String name, Object spec) {
            if(spec instanceof MultipartFileSpec) {
                return (MultipartFileSpec) spec;
            }

            if(spec instanceof File) {
                File file = (File) spec;
                String filename = file.getName();
                if(filename == null || filename.isEmpty()) {
                    filename = defaultFilename(name);
                }
                return new MultipartFileSpec(name, filename, spec, null, new LinkedHashMap<>());
            }

            if(spec instanceof InputStream) {
                return new MultipartFileSpec(name, defaultFilename(name), spec, null, new LinkedHashMap<>());
            }

            if(spec instanceof byte[] || spec instanceof String || spec instanceof Number || spec instanceof Boolean) {
                return new MultipartFileSpec(name, defaultFilename(name), spec, null, new LinkedHashMap<>());
            }

            if(spec instanceof List) {
                List<?> tuple = (List<?>) spec;
                String filename = tuple.size() >= 1 && tuple.get(0) != null ? tuple.get(0).toString() : defaultFilename(name);
                Object content = tuple.size() >= 2 ? tuple.get(1) : "";
                if((filename == null || filename.isEmpty()) && content instanceof File) {
                    String derived = ((File) content).getName();
                    if(derived != null && !derived.isEmpty()) {
                        filename = derived;
                    }
                }
                String contentType = tuple.size() >= 3 && tuple.get(2) != null ? tuple.get(2).toString() : null;
                Map<String, String> headers = tuple.size() >= 4 && tuple.get(3) instanceof Map ? toStringMap((Map) tuple.get(3)) : new LinkedHashMap<>();
                return new MultipartFileSpec(name, filename, content, contentType, headers);
            }

            if(spec instanceof Map) {
                Map map = (Map) spec;
                String filename = valueAsString(map, "filename", valueAsString(map, "fileName", defaultFilename(name)));
                Object content = map.containsKey("content") ? map.get("content")
                        : map.containsKey("file") ? map.get("file")
                        : map.containsKey("data") ? map.get("data")
                        : map.containsKey("value") ? map.get("value")
                        : map.containsKey("body") ? map.get("body") : "";
                String contentType = valueAsString(map, "content_type", valueAsString(map, "contentType",
                        valueAsString(map, "mime_type", valueAsString(map, "mimeType", null))));
                Map<String, String> headers = map.containsKey("headers") && map.get("headers") instanceof Map ? toStringMap((Map) map.get("headers")) : new LinkedHashMap<>();

                if(map.containsKey("name")) {
                    name = String.valueOf(map.get("name"));
                }

                if((filename == null || filename.isEmpty()) && content instanceof File) {
                    String derived = ((File) content).getName();
                    if(derived != null && !derived.isEmpty()) {
                        filename = derived;
                    }
                }

                return new MultipartFileSpec(name, filename != null ? filename : defaultFilename(name), content, contentType, headers);
            }

            return new MultipartFileSpec(name, defaultFilename(name), String.valueOf(spec), null, new LinkedHashMap<>());
        }

        private static Map<String, String> toStringMap(Map map) {
            Map<String, String> headers = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                if(k != null && v != null) {
                    headers.put(k.toString(), v.toString());
                }
            });
            return headers;
        }

        private static String valueAsString(Map map, String key, String defaultValue) {
            Object value = map.get(key);
            return value != null ? value.toString() : defaultValue;
        }

        private static String defaultFilename(String name) {
            return name == null || name.isEmpty() ? "file" : name;
        }
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
