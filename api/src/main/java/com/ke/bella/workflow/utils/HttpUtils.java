package com.ke.bella.workflow.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

public class HttpUtils {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(60 * 2, TimeUnit.SECONDS)
            .build();
    static {
        logging.setLevel(Level.BASIC);
    }

    public static int postJson(String url, Object data) {
        String json = JsonUtils.toJson(data);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (okhttp3.Response resp = client.newCall(request).execute()) {
            return resp.code();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T postJson(Map<String, String> headers, String url, String json, TypeReference<T> typeReference) {
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        return executePost(headers, url, requestBody, typeReference);
    }

    public static <T> T postFrom(Map<String, String> headers, String url, Map<String, String> params, TypeReference<T> typeReference) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        params.forEach(formBodyBuilder::add);
        RequestBody requestBody = formBodyBuilder.build();
        return executePost(headers, url, requestBody, typeReference);
    }

    public static <T> T get(String url, Map<String, String> queryParams, TypeReference<T> typeReference) {
        return get(null, url, queryParams, typeReference);
    }

    public static <T> T get(Map<String, String> headers, String url, Map<String, String> queryParams, TypeReference<T> typeReference) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        if(queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        String finalUrl = urlBuilder.build().toString();
        return executeRequest(headers, finalUrl, null, typeReference, "GET");
    }

    private static <T> T executePost(Map<String, String> headers, String url, RequestBody requestBody, TypeReference<T> typeReference) {
        return executeRequest(headers, url, requestBody, typeReference, "POST");
    }

    private static <T> T executeRequest(Map<String, String> headers, String url, RequestBody requestBody, TypeReference<T> typeReference,
            String method) {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        Response resp = null;
        ResponseBody respBody = null;
        try {
            if(headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            if("POST".equalsIgnoreCase(method) && requestBody != null) {
                requestBuilder.post(requestBody);
            } else {
                requestBuilder.get();
            }
            resp = client.newCall(requestBuilder.build()).execute();
            respBody = resp.body();
            if(!resp.isSuccessful()) {
                throw new IllegalStateException(
                        "failed to " + method.toLowerCase() + " to " + url + ": url: " + url + ", code: " + resp.code() + ", body: "
                                + (Objects.isNull(respBody) ? "" : respBody.string()));
            }
            if(Objects.isNull(respBody)) {
                return null;
            }
            String responseBodyStr = respBody.string();
            return JsonUtils.fromJson(responseBodyStr, typeReference);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if(respBody != null) {
                respBody.close();
            }
            if(resp != null) {
                resp.close();
            }
        }
    }
}
