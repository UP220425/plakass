package PlateRecognitionApp.plakass.ui.register

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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

        // Forzar fondo transparente en la vista
        dialogView.setBackgroundResource(android.R.color.transparent)

        val animationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieStatus)
        val textView = dialogView.findViewById<TextView>(R.id.tvStatusMessage)

        // Crear el AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Configurar la ventana del diálogo para que sea transparente
        dialog.window?.apply {
            // Fondo transparente
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Eliminar decoraciones si es necesario
            decorView.setBackgroundColor(Color.TRANSPARENT)

            // Configurar parámetros de la ventana
            val layoutParams = WindowManager.LayoutParams().apply {
                copyFrom(attributes)
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER

                // Opcional: configurar transparencia de fondo
                dimAmount = 0.5f

                // Para versiones más nuevas de Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    attributes.blurBehindRadius = 0
                }

                // Para versiones anteriores
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    flags = flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                }
            }
            attributes = layoutParams
        }

        // Configurar la animación Lottie
        animationView.apply {
            // Fondo transparente
            setBackgroundResource(android.R.color.transparent)

            // Habilitar optimizaciones
            enableMergePathsForKitKatAndAbove(true)

            // Opcional: ajustar escala si es necesario
            scale = 1.1f

            // Para evitar problemas de renderizado
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        // Configurar el texto
        textView.setTextColor(resources.getColor(android.R.color.white, null))
        textView.setBackgroundResource(R.drawable.rounded_background) // Asegúrate de tener este drawable

        // Mostrar el diálogo ANTES de cualquier animación
        dialog.show()

        // Animación de entrada suave
        dialogView.alpha = 0f
        dialogView.animate()
            .alpha(1f)
            .setDuration(300)
            .withStartAction {
                // Configurar la animación inicial
                animationView.setAnimation(R.raw.loading_hand)
                animationView.repeatCount = 0
                textView.text = "Creando usuario..."
            }
            .withEndAction {
                // Iniciar la animación después del fade in
                animationView.playAnimation()
            }
            .start()

        // Listener para cambiar a animación de éxito
        animationView.addAnimatorUpdateListener {
            if (animationView.progress >= 0.90f) {
                animationView.removeAllAnimatorListeners()

                animationView.post {
                    animationView.cancelAnimation()
                    animationView.setAnimation(R.raw.success_check)
                    textView.text = "¡Usuario creado!"
                    animationView.playAnimation()

                    // Programar el cierre y navegación
                    dialogView.postDelayed({
                        if (isAdded && findNavController().currentDestination?.id == R.id.registerFragment) {
                            // Animación de salida suave
                            dialogView.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    dialog.dismiss()
                                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                }
                                .start()
                        }
                    }, 1800)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}