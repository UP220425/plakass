package PlateRecognitionApp.plakass.ui.login

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentLoginBinding
import PlateRecognitionApp.plakass.utils.SessionManager
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import com.airbnb.lottie.LottieAnimationView

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

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

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }
    }

    private fun validateAndLogin() {

        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Ingresa tu correo"
            return
        }

        if (pass.isEmpty()) {
            binding.etPassword.error = "Ingresa tu contraseña"
            return
        }

        viewModel.login(email, pass) { ok, msg, data, token ->
            if (ok && data != null && token != null) {

                // Guardar token
                SessionManager.saveToken(requireContext(), token)

                // Guardar user_id
                val prefs = requireContext().getSharedPreferences("user_prefs", 0)
                prefs.edit()
                    .putString("user_id", data.id)
                    .apply()

                // 🔥 AQUÍ SE MANDA EL JOIN AUTOMÁTICO
                SocketHandler.registerUser(data.id)

                showDualAnimation()

            } else {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
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

        dialog.window?.setDimAmount(0.5f)
        dialog.show()

        dialogView.alpha = 0f
        dialogView.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        animationView.setAnimation(R.raw.loading_hand)
        textView.text = "Iniciando sesión..."
        animationView.playAnimation()

        animationView.addAnimatorUpdateListener {
            if (animationView.progress >= 0.90f) {

                animationView.clearAnimation()
                animationView.setAnimation(R.raw.success_check)
                textView.text = "¡Inicio exitoso!"
                animationView.playAnimation()

                dialogView.postDelayed({
                    dialog.dismiss()

                    if (isAdded &&
                        findNavController().currentDestination?.id == R.id.loginFragment
                    ) {
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                }, 1500)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
