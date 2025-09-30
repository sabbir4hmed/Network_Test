package com.sabbir.waltonbd.wcmsproductchecker.Api;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // WCMS API Base URL
    private static final String BASE_URL = "http://wcms.whildc.com/wcms/";

    // Authorization Header Value
    private static final String AUTH_TOKEN = "iF3Pxfslyai";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            // Logging Interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Authorization Interceptor
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws java.io.IOException {
                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", AUTH_TOKEN)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .build();
                    return chain.proceed(newRequest);
                }
            };

            // OkHttp Client with timeout settings and interceptors
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
