package com.worldmates.messenger.network

import android.util.Log
import com.worldmates.messenger.data.Constants
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Interceptor для добавления server_key и siteEncryptKey в каждый запрос.
 */
class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHttpUrl = originalRequest.url

        // Добавляем siteEncryptKey в качестве query-параметра 's'
        val newUrl = originalHttpUrl.newBuilder()
            .addQueryParameter("s", Constants.SITE_ENCRYPT_KEY)
            .build()

        // Если это POST запрос, добавляем server_key в тело
        val newRequest = if (originalRequest.method == "POST" && originalRequest.body is FormBody) {
            val formBody = originalRequest.body as FormBody
            val formBodyBuilder = FormBody.Builder()

            // Добавляем server_key первым
            formBodyBuilder.add("server_key", Constants.SERVER_KEY)

            // Копируем все существующие параметры
            for (i in 0 until formBody.size) {
                formBodyBuilder.add(formBody.name(i), formBody.value(i))
            }

            originalRequest.newBuilder()
                .url(newUrl)
                .post(formBodyBuilder.build())
                .build()
        } else {
            originalRequest.newBuilder()
                .url(newUrl)
                .build()
        }

        Log.d("API_REQUEST", "URL: ${newRequest.url}")
        Log.d("API_REQUEST", "Method: ${newRequest.method}")
        Log.d("API_REQUEST", "Headers: ${newRequest.headers}")

        return chain.proceed(newRequest)
    }
}

object RetrofitClient {

    // Логирование HTTP запросов
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("API_LOG", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor()) // Добавляем server_key сначала
        .addInterceptor(loggingInterceptor) // Логируем после модификации запроса
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Используем новый интерфейс
    val apiService: WorldMatesApi = retrofit.create(WorldMatesApi::class.java) // <--- ИЗМЕНЕНО
}