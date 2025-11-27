package PlateRecognitionApp.plakass.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.databinding.FragmentUserSettingsBinding
import PlateRecognitionApp.plakass.utils.SessionManager
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class UserSettingsFragment : Fragment() {

    private var _binding: FragmentUserSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
    }

    private fun loadUserDataFromAPI() {
        lifecycleScope.launch {
            val response = ApiClient.retrofit.getUser()

            if (response.isSuccessful && response.body()?.status == true) {
                val user = response.body()!!.data!!

                binding.tvUserName.text = user.name
                binding.tvUserEmail.text = user.email
                binding.etFullName.setText(user.name)
                binding.etEmail.setText(user.email)
                binding.etPhone.setText(user.phone ?: "")
            } else {
                Toast.makeText(requireContext(), "Error cargando datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfileChanges() {
        val fullName = binding.etFullName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()

        if (fullName.isEmpty() || email.isEmpty()) {
            showMessage("Completa todos los campos obligatorios")
            return
        }

        lifecycleScope.launch {
            val body = hashMapOf<String, Any>(
                "name" to fullName,
                "email" to email,
                "phone" to phone
            )

            val response = ApiClient.retrofit.updateUser(body)

            if (response.isSuccessful && response.body()?.status == true) {
                showMessage("Perfil actualizado correctamente")
                loadUserDataFromAPI()
            } else {
                showMessage("Error al actualizar perfil")
            }
        }
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        val currentPass = EditText(requireContext())
        currentPass.hint = "Contraseña actual"

        val newPass = EditText(requireContext())
        newPass.hint = "Nueva contraseña"

        layout.addView(currentPass)
        layout.addView(newPass)

        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar contraseña")
            .setView(layout)
            .setPositiveButton("Cambiar") { _, _ ->
                changePassword(currentPass.text.toString(), newPass.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changePassword(current: String, new: String) {
        lifecycleScope.launch {
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
