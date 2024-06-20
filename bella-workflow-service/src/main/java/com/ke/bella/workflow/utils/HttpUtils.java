package com.ke.bella.workflow.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.JsonUtils;

import okhttp3.FormBody;
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

    @Nullable
    public static <T> T postFrom(Map<String, String> headers, String url, Map<String, String> params, TypeReference<T> typeReference) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        if(headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        params.forEach(formBodyBuilder::add);
        requestBuilder.post(formBodyBuilder.build());
        Response resp = null;
        ResponseBody respBody = null;
        try {
            resp = client.newCall(requestBuilder.build()).execute();
            respBody = resp.body();
            if(!resp.isSuccessful()) {
                throw new IllegalStateException("failed to post to " + url + ": url: " + url + ", code: " + resp.code() + ", body: "
                        + (Objects.isNull(respBody) ? "" : respBody.string()));
            }
            if(Objects.isNull(respBody)) {
                return null;
            }
            String responseBodyStr = respBody.string();
            return JsonUtils.fromJson(responseBodyStr, typeReference);
        } catch (IOException e) {
            if(resp != null) {
                resp.close();
            }
            if(respBody != null) {
                respBody.close();
            }
            throw new UncheckedIOException(e);
        }
    }
}
