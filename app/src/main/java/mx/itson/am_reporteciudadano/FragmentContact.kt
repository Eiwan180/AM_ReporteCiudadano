package mx.itson.am_reporteciudadano

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mx.itson.am_reporteciudadano.databinding.ActivityFragmentContactBinding

class FragmentContact : Fragment() {

    private var _binding: ActivityFragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Abrir Google Maps
        binding.btnAbrirMapa.setOnClickListener {
            val uri = Uri.parse("geo:0,0?q=Palacio+Municipal+Tangamandapio+Michoacan")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // Enviar Correo
        binding.btnEnviarCorreo.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:contacto@tangamandapio.gob.mx")
                putExtra(Intent.EXTRA_SUBJECT, "Consulta Ciudadana")
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
