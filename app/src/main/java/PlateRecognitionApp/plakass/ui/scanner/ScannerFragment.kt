    package PlateRecognitionApp.plakass.ui.scanner

    import android.Manifest
    import android.content.Context
    import android.content.pm.PackageManager
    import android.os.Bundle
    import android.os.VibrationEffect
    import android.os.Vibrator
    import android.os.VibratorManager
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
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

            binding.btnEnterTicket.setOnClickListener {
                findNavController().navigate(R.id.action_scannerFragment_to_manualEntryFragment)
            }
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
                        findNavController().popBackStack()
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
