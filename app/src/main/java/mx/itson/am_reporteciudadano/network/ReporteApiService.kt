package mx.itson.am_reporteciudadano.network

import mx.itson.am_reporteciudadano.models.Reporte
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ReporteApiService {
    @POST("reporte.php")
    fun enviarReporte(
        @Header("Authorization") token: String,
        @Body reporte: Reporte
    ): Call<Void>

    companion object {
        private const val BASE_URL = "https://mcaconsultores.com.mx/apireporte/"

        fun create(): ReporteApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ReporteApiService::class.java)
        }
    }
}
