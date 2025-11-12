package PlateRecognitionApp.plakass.ui.scanner

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentManualEntryBinding
import com.airbnb.lottie.LottieAnimationView

class ManualEntryFragment : Fragment() {

    private var _binding: FragmentManualEntryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSubmit.setOnClickListener {
            val ticketNumber = binding.etTicketNumber.text.toString()
            if (ticketNumber.isNotEmpty()) {
                showSuccessDialog()
            }
        }
    }

    /** 🔹 Muestra el pop-up animado con Lottie */
    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_ticket_success, null)

        val animationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieSuccess)
        animationView.setAnimation(R.raw.gate_animation)
        animationView.playAnimation()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        // Opcional: cerrar automáticamente después de 3 segundos
        dialogView.postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
                findNavController().popBackStack()
            }
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
