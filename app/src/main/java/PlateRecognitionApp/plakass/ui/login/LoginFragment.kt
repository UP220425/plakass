package PlateRecognitionApp.plakass.ui.login

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentLoginBinding
import PlateRecognitionApp.plakass.utils.SessionManager
import PlateRecognitionApp.plakass.utils.NetworkUtils
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import PlateRecognitionApp.plakass.data.local.repository.ParkingLocalRepository
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.launch

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

        val token = SessionManager.getToken(requireContext())
        val hasInternet = NetworkUtils.isConnected(requireContext())

        // 🔥 SI YA HAY TOKEN → PASAR DIRECTO A HOME SIEMPRE
        // (Con o sin internet)
        if (!token.isNullOrBlank()) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // Si no había token, ahora sí carga UI normal:
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
                prefs.edit().putString("user_id", data.id).apply()

                // Limpiar base de datos local
                lifecycleScope.launch {
                    val repo = ParkingLocalRepository(requireContext())
                    repo.clearActiveSession()
                    repo.clearHistory()
                }

                // Socket → join
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
        textView.setBackgroundResource(R.drawable.rounded_background)

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
                textView.text = "Iniciando sesión..."
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
                    textView.text = "¡Inicio exitoso!"
                    animationView.playAnimation()

                    // Programar el cierre y navegación
                    dialogView.postDelayed({
                        if (isAdded && findNavController().currentDestination?.id == R.id.loginFragment) {
                            // Animación de salida suave
                            dialogView.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    dialog.dismiss()
                                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                                }
                                .start()
                        }
                    }, 1500)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}