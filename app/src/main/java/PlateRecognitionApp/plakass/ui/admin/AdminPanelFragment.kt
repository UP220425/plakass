package PlateRecognitionApp.plakass.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentAdminPanelBinding

class AdminPanelFragment : Fragment() {

    private lateinit var binding: FragmentAdminPanelBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminPanelBinding.inflate(inflater, container, false)

        binding.btnAdminQR.setOnClickListener {
            findNavController().navigate(R.id.action_adminPanelFragment_to_adminQRFragment)
        }

        binding.btnAdminNoApp.setOnClickListener {
            findNavController().navigate(R.id.action_adminPanelFragment_to_noAppVehiclesFragment)
        }

        return binding.root
    }
}
