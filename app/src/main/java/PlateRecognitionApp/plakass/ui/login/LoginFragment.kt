package PlateRecognitionApp.plakass.ui.login

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentLoginBinding
import com.airbnb.lottie.LottieAnimationView

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ir al registro
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Mostrar animación de login al presionar el botón
        binding.btnLogin.setOnClickListener {
            showDualAnimation()
        }
    }

    private fun showDualAnimation() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_login_loading, null)
        val animationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieStatus)
        val textView = dialogView.findViewById<TextView>(R.id.tvStatusMessage)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Oscurece el fondo un poco
        dialog.window?.setDimAmount(0.5f)
        dialog.show()

        // 🔹 Efecto de aparición suave (fade in del cuadro)
        dialogView.alpha = 0f
        dialogView.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        // 🔹 Primera animación: la manita de "cargando"
        animationView.setAnimation(R.raw.loading_hand)
        animationView.playAnimation()
        textView.text = "Iniciando sesión..."

        // 🔹 Cuando termina la primera animación, cambiar a la palomita verde
        animationView.addAnimatorUpdateListener { animator ->
            if (animationView.progress >= 0.99f) {
                // Limpiamos y cargamos la animación de éxito
                animationView.clearAnimation()
                animationView.setAnimation(R.raw.success_check)
                textView.text = "¡Inicio exitoso!"
                animationView.playAnimation()

                // Esperamos un momento antes de cerrar y navegar
                dialogView.postDelayed({
                    dialog.dismiss()

                    // Solo navega si SIGUE en el LoginFragment (evita crash)
                    val currentDest = findNavController().currentDestination?.id
                    if (currentDest == R.id.loginFragment) {
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
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
