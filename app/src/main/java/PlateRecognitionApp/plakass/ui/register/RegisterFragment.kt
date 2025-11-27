package PlateRecognitionApp.plakass.ui.register

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
import PlateRecognitionApp.plakass.databinding.FragmentRegisterBinding
import com.airbnb.lottie.LottieAnimationView

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

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

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {

        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val pass = binding.etPassword.text.toString()
        val pass2 = binding.etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            binding.etName.error = "Ingresa tu nombre"
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Ingresa tu correo"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Correo inválido"
            return
        }

        if (phone.length != 10) {
            binding.etPhone.error = "Debe tener 10 dígitos"
            return
        }

        if (pass.length < 6) {
            binding.etPassword.error = "Mínimo 6 caracteres"
            return
        }

        if (pass != pass2) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            return
        }

        if (!binding.cbTerms.isChecked) {
            Toast.makeText(requireContext(), "Debes aceptar los términos", Toast.LENGTH_SHORT).show()
            return
        }

        // BACKEND
        viewModel.register(name, email, phone, pass) { ok, msg ->
            if (ok) {
                showRegisterAnimation()
            } else {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
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

        dialog.window?.setDimAmount(0.5f)
        dialog.show()

        dialogView.alpha = 0f
        dialogView.animate().alpha(1f).setDuration(400).start()

        animationView.setAnimation(R.raw.loading_hand)
        animationView.repeatCount = 0
        animationView.playAnimation()
        textView.text = "Creando usuario..."

        animationView.addAnimatorUpdateListener {
            if (animationView.progress >= 0.90f) {
                animationView.clearAnimation()
                animationView.setAnimation(R.raw.success_check)
                textView.text = "¡Usuario creado!"
                animationView.playAnimation()

                dialogView.postDelayed({
                    if (isAdded && findNavController().currentDestination?.id == R.id.registerFragment) {
                        dialog.dismiss()
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                }, 1800)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
