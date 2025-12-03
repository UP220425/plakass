package PlateRecognitionApp.plakass.data.api

import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://plakass-api-amhuasfpadhwdvfm.canadacentral-01.azurewebsites.net/"

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()
    }

    val retrofit: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Helper functions para crear RequestBody - SIN "companion"
    fun createTextRequestBody(text: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), text)
    }

    fun createMultipartPart(partName: String, file: java.io.File): MultipartBody.Part {
        val requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file)
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }
}