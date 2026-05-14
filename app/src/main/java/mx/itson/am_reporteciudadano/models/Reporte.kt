package mx.itson.am_reporteciudadano.models

import com.google.gson.annotations.SerializedName

data class Reporte(
    @SerializedName("nombre_interesado")
    val nombre: String,
    
    @SerializedName("direccion")
    val direccion: String,
    
    @SerializedName("colonia")
    val colonia: String,
    
    @SerializedName("celular")
    val celular: String,
    
    @SerializedName("correo")
    val correo: String,
    
    @SerializedName("tipo")
    val tipo: String,
    
    @SerializedName("descripcion")
    val descripcion: String,
    
    @SerializedName("imagen")
    val imagen: String? = null
)
