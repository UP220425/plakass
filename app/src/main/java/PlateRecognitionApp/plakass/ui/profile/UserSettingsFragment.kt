package PlateRecognitionApp.plakass.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.databinding.FragmentUserSettingsBinding
import PlateRecognitionApp.plakass.utils.SessionManager
import java.io.File
import java.io.FileOutputStream
import PlateRecognitionApp.plakass.data.model.UpdateUserResponse

class UserSettingsFragment : Fragment() {

    private var _binding: FragmentUserSettingsBinding? = null
    private val binding get() = _binding!!

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadUserDataFromAPI()
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSaveProfile.setOnClickListener { saveProfileChanges() }
        binding.optionChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.optionLogout.setOnClickListener { logout() }
        binding.ivProfilePicture.setOnClickListener { openImagePicker() }
    }

    private fun loadUserDataFromAPI() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.retrofit.getUser()

                if (response.isSuccessful && response.body()?.status == true) {
                    val user = response.body()!!.data!!

                    // Setear datos en los campos
                    binding.tvUserName.text = user.name
                    binding.tvUserEmail.text = user.email
                    binding.etFullName.setText(user.name)
                    binding.etEmail.setText(user.email)
                    binding.etPhone.setText(user.phone ?: "")

                    // Mostrar foto de perfil si está disponible
                    user.profile_picture?.let { profilePictureUrl ->
                        Glide.with(requireContext())
                            .load(profilePictureUrl)
                            .placeholder(R.drawable.ic_person)  // Imagen por defecto
                            .error(R.drawable.ic_person)  // Imagen si hay error
                            .circleCrop()  // Esto recorta la imagen para que se vea como un círculo
                            .into(binding.ivProfilePicture)
                    }
                } else {
                    showMessage("Error cargando datos del usuario")
                }
            } catch (e: Exception) {
                showMessage("Error de conexión: ${e.message}")
                Log.e("UserSettings", "loadUserDataFromAPI error", e)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            // Mostrar imagen seleccionada con Glide para mejor calidad
            selectedImageUri?.let { uri ->
                Glide.with(requireContext())
                    .load(uri)
                    .centerCrop()
                    .circleCrop()  // <-- ¡ESTO ES CLAVE!
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivProfilePicture)
            }
        }
    }

    private fun saveProfileChanges() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty()) {
            showMessage("Completa todos los campos obligatorios")
            return
        }

        lifecycleScope.launch {
            try {
                val response = if (selectedImageUri != null) {
                    // CON imagen: usar updateUserWithImage
                    saveWithImage(fullName, email, phone)
                } else {
                    // SIN imagen: usar updateUser
                    saveWithoutImage(fullName, email, phone)
                }

                if (response.isSuccessful && response.body()?.status == true) {
                    showMessage("Perfil actualizado correctamente")
                    // Recargar datos del API
                    loadUserDataFromAPI()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    showMessage("Error al actualizar perfil")
                    Log.e("UserSettings", "API Error: $errorBody")
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}")
                Log.e("UserSettings", "saveProfileChanges error", e)
            }
        }
    }

    private suspend fun saveWithImage(fullName: String, email: String, phone: String): retrofit2.Response<UpdateUserResponse> {
        // Usar las funciones de ApiClient
        val nameBody = ApiClient.createTextRequestBody(fullName)
        val emailBody = ApiClient.createTextRequestBody(email)
        val phoneBody = ApiClient.createTextRequestBody(phone)

        // Convertir URI a archivo
        val imageFile = selectedImageUri?.let { uriToFile(it) }

        // Crear MultipartBody.Part para la imagen
        val imagePart = imageFile?.let {
            ApiClient.createMultipartPart("profile_picture", it)
        }

        // IMPORTANTE: Aquí NO podemos pasar null, así que verificamos
        return if (imagePart != null) {
            ApiClient.retrofit.updateUserWithImage(
                name = nameBody,
                email = emailBody,
                phone = phoneBody,
                profile_picture = imagePart
            )
        } else {
            // Si no se pudo crear la imagen, usar el método sin imagen
            saveWithoutImage(fullName, email, phone)
        }
    }

    private suspend fun saveWithoutImage(fullName: String, email: String, phone: String): retrofit2.Response<UpdateUserResponse> {
        val body = hashMapOf<String, Any>(
            "name" to fullName,
            "email" to email,
            "phone" to phone
        )
        return ApiClient.retrofit.updateUser(body)
    }

    private fun uriToFile(uri: Uri): File {
        val context = requireContext()
        val contentResolver = context.contentResolver

        // Crear archivo temporal con nombre único
        val timeStamp = System.currentTimeMillis()
        val file = File(context.cacheDir, "profile_${timeStamp}.jpg")

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
            }

            Log.d("UserSettings", "Archivo creado: ${file.absolutePath}, tamaño: ${file.length()} bytes")
            return file
        } catch (e: Exception) {
            Log.e("UserSettings", "Error al convertir URI a File", e)
            throw e
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val currentPass = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val newPass = dialogView.findViewById<EditText>(R.id.etNewPassword)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val currentPassword = currentPass.text.toString()
            val newPassword = newPass.text.toString()

            if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                changePassword(currentPassword, newPassword)
                dialog.dismiss()
            } else {
                showMessage("Por favor ingresa las contraseñas")
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun changePassword(current: String, new: String) {
        lifecycleScope.launch {
            try {
                val body = hashMapOf<String, Any>(
                    "current_password" to current,
                    "new_password" to new
                )

                val response = ApiClient.retrofit.changePassword(body)

                if (response.isSuccessful && response.body()?.status == true) {
                    showMessage("Contraseña cambiada correctamente")
                } else {
                    showMessage("Error al cambiar contraseña")
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}")
                Log.e("UserSettings", "changePassword error", e)
            }
        }
    }

    private fun logout() {
        SessionManager.logout(requireContext())
        findNavController().navigate(R.id.action_global_loginFragment)
    }

    private fun showMessage(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}