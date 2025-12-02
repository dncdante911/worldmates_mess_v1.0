package com.worldmates.messenger.network

import com.worldmates.messenger.data.Constants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Interceptor для добавления siteEncryptKey в каждый запрос.
 */
class SiteKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHttpUrl = originalRequest.url

        // Добавляем siteEncryptKey в качестве query-параметра 's'
        val newUrl = originalHttpUrl.newBuilder()
            .addQueryParameter("s", Constants.SITE_ENCRYPT_KEY)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}

object RetrofitClient {

    private val client = OkHttpClient.Builder()
        .addInterceptor(SiteKeyInterceptor())
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