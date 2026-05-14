package mx.itson.am_reporteciudadano

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mx.itson.am_reporteciudadano.databinding.ActivityReportFragmentBinding
import mx.itson.am_reporteciudadano.models.Reporte
import mx.itson.am_reporteciudadano.network.ReporteApiService
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

class ReportFragment : Fragment() {
    private var _binding: ActivityReportFragmentBinding? = null
    private val binding get() = _binding!!

    private var imageBitmap: Bitmap? = null
    
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { mostrarVistaPrevia(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val bmp = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                imageBitmap = bmp
                mostrarVistaPrevia(bmp)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityReportFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarDatosDesdeJSON()

        binding.btnAdjuntarFoto.setOnClickListener { mostrarOpcionesFoto() }
        binding.btnEnviar.setOnClickListener { validarYEnviar() }
    }

    private fun cargarDatosDesdeJSON() {
        try {
            val jsonString = requireContext().assets.open("data.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val tiposArray = jsonObject.getJSONArray("tipos_reporte")
            val tiposList = mutableListOf<String>()
            for (i in 0 until tiposArray.length()) {
                tiposList.add(tiposArray.getString(i))
            }
            val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposList)
            binding.actTipoReporte.setAdapter(adapterTipos)

            val coloniasArray = jsonObject.getJSONArray("colonias")
            val coloniasList = mutableListOf<String>()
            for (i in 0 until coloniasArray.length()) {
                coloniasList.add(coloniasArray.getString(i))
            }
            val adapterColonias = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, coloniasList)
            binding.actColonia.setAdapter(adapterColonias)

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback a los strings de recursos si el JSON falla
            configurarSpinners()
        }
    }

    private fun configurarSpinners() {
        val colonias = resources.getStringArray(R.array.colonias_array)
        val adapterColonias = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colonias)
        binding.actColonia.setAdapter(adapterColonias)

        val tipos = resources.getStringArray(R.array.tipos_reporte_array)
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        binding.actTipoReporte.setAdapter(adapterTipos)
    }

    private fun mostrarOpcionesFoto() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Adjuntar evidencia")
            .setItems(arrayOf("Tomar foto", "Elegir de galería")) { _, which ->
                if (which == 0) {
                    verificarPermisoCamara()
                } else {
                    galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                }
            }.show()
    }

    private fun verificarPermisoCamara() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun abrirCamara() {
        cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    private fun mostrarVistaPrevia(bmp: Bitmap) {
        binding.imgPreview.setImageBitmap(bmp)
        binding.cardImgPreview.visibility = View.VISIBLE
        binding.btnAdjuntarFoto.visibility = View.GONE
    }

    private fun validarYEnviar() {
        val nombre = binding.etNombre.text.toString()
        val celular = binding.etCelular.text.toString()
        val correo = binding.etCorreo.text.toString()
        val direccion = binding.etDireccion.text.toString()
        val descripcion = binding.etDescripcion.text.toString()
        val colonia = binding.actColonia.text.toString()
        val tipoReporte = binding.actTipoReporte.text.toString()

        if (nombre.isEmpty() || descripcion.isEmpty() || colonia.isEmpty() || tipoReporte.isEmpty() || correo.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        enviarReporteReal(nombre, celular, correo, direccion, descripcion, colonia, tipoReporte)
    }

    private fun enviarReporteReal(nombre: String, celular: String, correo: String, direccion: String, descripcion: String, colonia: String, tipo: String) {
        val base64Image = imageBitmap?.let { bitmapToBase64(it) }
        // El API espera el prefijo "data:image/png;base64," si es una imagen
        val finalImage = base64Image?.let { "data:image/png;base64,$it" }
        
        val reporte = Reporte(nombre, direccion, colonia, celular, correo, tipo, descripcion, finalImage)

        binding.btnEnviar.isEnabled = false
        Toast.makeText(requireContext(), "Enviando reporte...", Toast.LENGTH_SHORT).show()

        val token = "Bearer a0f4dcad-5903-482f-8982-88ec8bc6156e"
        ReporteApiService.create().enviarReporte(token, reporte).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                binding.btnEnviar.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Reporte enviado exitosamente", Toast.LENGTH_LONG).show()
                    limpiarFormulario()
                } else {
                    Toast.makeText(requireContext(), "Error al enviar: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                binding.btnEnviar.isEnabled = true
                Toast.makeText(requireContext(), "Fallo de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Comprimir para reducir tamaño (Google Apps Script tiene límites)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(byteArray)
        } else {
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        }
    }

    private fun limpiarFormulario() {
        binding.etNombre.text?.clear()
        binding.etCelular.text?.clear()
        binding.etCorreo.text?.clear()
        binding.etDireccion.text?.clear()
        binding.etDescripcion.text?.clear()
        binding.actColonia.text?.clear()
        binding.actTipoReporte.text?.clear()

        // Resetear imagen
        imageBitmap = null
        binding.cardImgPreview.visibility = View.GONE
        binding.btnAdjuntarFoto.visibility = View.VISIBLE

        // Quitar el foco
        binding.etNombre.clearFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
