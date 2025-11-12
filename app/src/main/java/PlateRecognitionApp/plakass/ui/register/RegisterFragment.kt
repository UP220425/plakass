package PlateRecognitionApp.plakass.ui.register

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentRegisterBinding
import com.airbnb.lottie.LottieAnimationView

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Volver al login
        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // 🔹 Mostrar animación al registrarse
        binding.btnRegister.setOnClickListener {
            showRegisterAnimation()
        }
    }

    private fun showRegisterAnimation() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_login_loading, null)
        val animationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieStatus)
        val textView = dialogView.findViewById<TextView>(R.id.tvStatusMessage)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Oscurecer ligeramente el fondo
        dialog.window?.setDimAmount(0.5f)
        dialog.show()

        // 🔹 Efecto fade-in del cuadro
        dialogView.alpha = 0f
        dialogView.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        // 🔹 Primera animación: manita (simula creación)
        animationView.setAnimation(R.raw.loading_hand)
        animationView.playAnimation()
        textView.text = "Creando usuario..."

        // 🔹 Al terminar, cambia a la palomita de éxito
        animationView.addAnimatorUpdateListener {
            if (animationView.progress >= 0.99f) {
                animationView.clearAnimation()
                animationView.setAnimation(R.raw.success_check)
                textView.text = "¡Usuario creado exitosamente!"
                animationView.playAnimation()

                // Espera 2 segundos y regresa al login
                dialogView.postDelayed({
                    dialog.dismiss()

                    // ✅ Verificamos que el fragmento siga activo antes de navegar
                    if (isAdded && findNavController().currentDestination?.id == R.id.registerFragment) {
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                }, 2000)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
