package com.ke.bella.workflow.utils;

import java.io.IOException;

import com.ke.bella.workflow.JsonUtils;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

public class HttpUtils {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
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
}
