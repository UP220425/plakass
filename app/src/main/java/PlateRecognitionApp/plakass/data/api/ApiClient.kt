package PlateRecognitionApp.plakass.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://plakass-api-amhuasfpadhwdvfm.canadacentral-01.azurewebsites.net/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .build()

    val retrofit: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}