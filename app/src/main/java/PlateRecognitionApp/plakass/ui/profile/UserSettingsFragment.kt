package PlateRecognitionApp.plakass.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentUserSettingsBinding

class UserSettingsFragment : Fragment() {

    private var _binding: FragmentUserSettingsBinding? = null
    private val binding get() = _binding!!

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
        loadUserData()
    }

    private fun setupClickListeners() {
        // Botón de regresar
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Cambiar foto de perfil
        binding.tvChangePhoto.setOnClickListener {
            // Aquí puedes implementar la lógica para cambiar la foto
            showMessage("Funcionalidad para cambiar foto en desarrollo")
        }

        // Guardar cambios del perfil
        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        // Cambiar contraseña
        binding.optionChangePassword.setOnClickListener {
            showMessage("Funcionalidad para cambiar contraseña en desarrollo")
        }

        // Cerrar sesión
        binding.optionLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        // Aquí cargarías los datos reales del usuario
        // Por ahora usamos datos de ejemplo
        binding.tvUserName.text = "Juan Pérez"
        binding.tvUserEmail.text = "juan.perez@email.com"
        binding.etFullName.setText("Juan Pérez")
        binding.etEmail.setText("juan.perez@email.com")
        binding.etPhone.setText("+52 123 456 7890")
    }

    private fun saveProfileChanges() {
        val fullName = binding.etFullName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()

        if (fullName.isEmpty() || email.isEmpty()) {
            showMessage("Por favor, completa todos los campos obligatorios")
            return
        }

        // Aquí iría la lógica para guardar los cambios en tu base de datos/API
        showMessage("Cambios guardados exitosamente")

        // Actualizar la UI con los nuevos datos
        binding.tvUserName.text = fullName
        binding.tvUserEmail.text = email
    }

    private fun logout() {
        // Aquí iría la lógica para cerrar sesión
        // Por ejemplo, limpiar SharedPreferences, tokens, etc.

        // Navegar al login
        findNavController().navigate(R.id.action_global_loginFragment)
    }

    private fun showMessage(message: String) {
        // Puedes usar Toast, Snackbar, etc.
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}