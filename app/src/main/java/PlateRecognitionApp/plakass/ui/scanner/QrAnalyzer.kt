package PlateRecognitionApp.plakass.ui.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image ?: return imageProxy.close()

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val raw = barcode.rawValue ?: continue
                    onQrDetected(raw)
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener { }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
