package PlateRecognitionApp.plakass.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import PlateRecognitionApp.plakass.data.model.UserData
import PlateRecognitionApp.plakass.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repo = UserRepository()

    fun login(
        email: String,
        password: String,
        callback: (Boolean, String, UserData?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repo.login(email, password)

                if (response.isSuccessful && response.body()?.status == true) {

                    val body = response.body()!!
                    val data = body.data          // <- usuario
                    val token = body.token        // <- token JWT

                    callback(true, "OK", data, token)

                } else {
                    callback(false, "Credenciales incorrectas", null, null)
                }

            } catch (e: Exception) {
                callback(false, "Error: ${e.message}", null, null)
            }
        }
    }
}
