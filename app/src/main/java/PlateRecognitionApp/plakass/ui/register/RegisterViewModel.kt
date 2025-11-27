package PlateRecognitionApp.plakass.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import PlateRecognitionApp.plakass.data.repository.UserRepository
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val repo = UserRepository()

    fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repo.register(name, email, phone, password)

                if (response.isSuccessful) {
                    callback(true, response.body()?.message ?: "Registro exitoso")
                } else {
                    callback(false, "Error en el servidor")
                }

            } catch (e: Exception) {
                callback(false, "Error: ${e.message}")
            }
        }
    }
}
