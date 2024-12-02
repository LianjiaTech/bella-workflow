package com.ke.bella.workflow.utils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import com.theokanning.openai.client.AuthenticationInterceptor;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

@Slf4j
public class OpenAiUtils {
    static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    static long DEFAULT_READ_TIMEOUT_SECONDS = 60 * 5L;
    static TimeUnit DEFAULT_READ_TIMEOUT_UNIT = TimeUnit.SECONDS;
    static OkHttpClient client;
    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(Configs.TASK_THREAD_NUMS);
        dispatcher.setMaxRequestsPerHost(Configs.TASK_THREAD_NUMS);

        client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(Configs.TASK_THREAD_NUMS, 60, TimeUnit.SECONDS))
                .build();
    }

    public static OpenAiService defaultOpenAiService(String token, long readTimeout, TimeUnit unit) {
        ObjectMapper mapper = OpenAiService.defaultObjectMapper();
        Map<String, String> transHeaders = BellaContext.getTransHeaders();

        OkHttpClient curClient = client.newBuilder()
                .addInterceptor(new AuthenticationInterceptor(token) {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        LOGGER.info(token);
                        Request request = chain.request()
                                .newBuilder()
                                .header("Authorization", token)
                                .build();
                        return chain.proceed(request);
                    }
                })
                .addInterceptor(chain -> {
                    Request.Builder requestBuilder = chain.request().newBuilder();
                    Optional.ofNullable(transHeaders).ifPresent(map -> map.forEach(requestBuilder::addHeader));
                    return chain.proceed(requestBuilder.build());
                })
                .readTimeout(readTimeout, unit).build();

        Retrofit retrofit = OpenAiService.defaultRetrofit(curClient, mapper, Configs.OPEN_API_BASE);
        OpenAiApi openAiApi = retrofit.create(OpenAiApi.class);

        ExecutorService executorService = client.dispatcher().executorService();

        return new OpenAiService(openAiApi, executorService);
    }

    public static OpenAiService defaultOpenAiService(String token) {
        return defaultOpenAiService(token, DEFAULT_READ_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_UNIT);
    }
}
