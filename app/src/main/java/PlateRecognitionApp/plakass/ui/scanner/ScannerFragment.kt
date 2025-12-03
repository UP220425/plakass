package PlateRecognitionApp.plakass.ui.scanner

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.databinding.FragmentScannerBinding
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var qrFound = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupClickListeners()
        setupBackPressedHandler()
        setupCamera()
    }

    // -----------------------------------------------------------
    // BOTONES
    // -----------------------------------------------------------

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Se eliminó el botón de ingreso manual
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            }
        )
    }

    // -----------------------------------------------------------
    // CÁMARA
    // -----------------------------------------------------------

    private fun setupCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            // PREVIEW
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // ANALYZER
            val qrAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrAnalyzer { qr ->
                        if (!qrFound) {
                            qrFound = true
                            vibrate()
                            handleQrResult(qr)
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    qrAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // -----------------------------------------------------------
    // VIBRAR AL DETECTAR QR
    // -----------------------------------------------------------

    private fun vibrate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                // Android 12+
                val vm = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vm.defaultVibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                // Android 8 - 11
                val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    // Android 5 - 7.1 (API 21-25)
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
        } catch (_: Exception) {}
    }

    // -----------------------------------------------------------
    // ANIMACIÓN MEJORADA
    // -----------------------------------------------------------

    private fun showQrSuccessAnimation(message: String = "QR escaneado exitosamente") {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_ticket_success, null)

        // Forzar fondo transparente en la vista
        dialogView.setBackgroundResource(android.R.color.transparent)

        val animationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieSuccess)
        val textView = dialogView.findViewById<TextView>(R.id.tvSuccessMessage)

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
        textView.text = message

        // Mostrar el diálogo ANTES de cualquier animación
        dialog.show()

        // Animación de entrada suave
        dialogView.alpha = 0f
        dialogView.animate()
            .alpha(1f)
            .setDuration(300)
            .withStartAction {
                // Configurar la animación inicial
                animationView.setAnimation(R.raw.gate_animation)
            }
            .withEndAction {
                // Iniciar la animación después del fade in
                animationView.playAnimation()
            }
            .start()

        // Cerrar automáticamente después de 3 segundos
        dialogView.postDelayed({
            if (dialog.isShowing) {
                // Animación de salida suave
                dialogView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        dialog.dismiss()
                        findNavController().popBackStack()
                    }
                    .start()
            }
        }, 3000)
    }

    // -----------------------------------------------------------
    // ENVIAR QR AL SERVIDOR
    // -----------------------------------------------------------

    private fun handleQrResult(qr: String) {
        lifecycleScope.launch {

            binding.txtStatus?.text = "QR detectado, procesando..."

            try {
                val body = hashMapOf<String, Any>(
                    "qr_code" to qr
                )

                val res = ApiClient.retrofit.registerScan(body)

                if (res.isSuccessful && res.body()?.status == true) {
                    binding.txtStatus?.text = "QR enviado ✓"

                    // Mostrar animación de éxito
                    showQrSuccessAnimation()

                } else {
                    binding.txtStatus?.text = "QR inválido"
                    qrFound = false
                }

            } catch (e: Exception) {
                binding.txtStatus?.text = "Error de conexión"
                qrFound = false
            }
        }
    }

    // -----------------------------------------------------------
    // PERMISOS
    // -----------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startCamera()

        } else {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}