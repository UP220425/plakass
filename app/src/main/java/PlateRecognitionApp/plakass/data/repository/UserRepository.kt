package PlateRecognitionApp.plakass.data.repository

import PlateRecognitionApp.plakass.data.api.ApiClient

class UserRepository {

    private val api = ApiClient.retrofit

    suspend fun register(name: String, email: String, phone: String, password: String) =
        api.register(
            hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "password" to password
            )
        )

    suspend fun login(email: String, password: String) =
        api.login(
            hashMapOf(
                "email" to email,
                "password" to password
            )
        )
}