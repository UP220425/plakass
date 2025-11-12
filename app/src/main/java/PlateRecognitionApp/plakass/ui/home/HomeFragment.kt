package PlateRecognitionApp.plakass.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserData()
        setupCardClickListeners()
    }

    private fun setupClickListeners() {
        // Navegación a configuración de usuario al hacer click en el icono de perfil
        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_userSettingsFragment)
        }
    }

    private fun loadUserData() {
        // Aquí deberías obtener el nombre del usuario de tu fuente de datos
        // Por ejemplo: SharedPreferences, Base de datos, ViewModel, etc.

        // Ejemplo con datos estáticos - reemplaza con tu lógica real
        val userName = getUserNameFromPreferences()

        if (userName.isNotEmpty()) {
            binding.tvUserName.text = userName
        } else {
            binding.tvUserName.text = "Usuario"
        }
    }

    private fun getUserNameFromPreferences(): String {
        // Ejemplo de cómo obtener el nombre del usuario
        // Reemplaza esto con tu implementación real
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", 0)
        return sharedPreferences.getString("user_name", "") ?: ""
    }

    private fun setupCardClickListeners() {
        // Configurar los click listeners para las cards del home
        // Card 1 - Escanear Ticket
        binding.cardEscanear.setOnClickListener {
            // Navegar al fragmento de escaneo
            showMessage("Funcionalidad de escaneo en desarrollo")
        }

        // Card 2 - Historial
        binding.cardHistorial.setOnClickListener {
            // Navegar al fragmento de historial
            showMessage("Funcionalidad de historial en desarrollo")
        }

        // Card 3 - Vehículos
        binding.cardVehiculos.setOnClickListener {
            // Navegar al fragmento de vehículos
            showMessage("Funcionalidad de vehículos en desarrollo")
        }

        // Card 4 - Configuración
        binding.cardConfiguracion.setOnClickListener {
            // Navegar a configuración (igual que el icono de perfil)
            findNavController().navigate(R.id.action_homeFragment_to_userSettingsFragment)
        }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}