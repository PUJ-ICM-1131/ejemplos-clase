package com.icm.maps.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationHelper(private val context: Context) {

    internal val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Obtiene la ubicación actual del usuario de manera asíncrona.
     */
    @SuppressLint("MissingPermission") // Asegúrate de manejar permisos antes de llamar esta función
    fun registerLocationUpdates(callback: (LatLng) -> Unit): LocationCallback {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Intervalo de actualización en ms
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    callback(LatLng(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return locationCallback // Para poder detener las actualizaciones luego
    }

    /** Convierte una dirección sin bloquear el hilo de la interfaz. */
    suspend fun getLatLngFromAddress(address: String): Pair<Double, Double>? {
        if (address.isBlank()) return null
        return geocode(address = address).firstOrNull()?.let { it.latitude to it.longitude }
    }

    /** Convierte coordenadas en dirección sin bloquear el hilo de la interfaz. */
    suspend fun getAddressFromLatLng(latitude: Double, longitude: Double): String? =
        geocode(latitude = latitude, longitude = longitude).firstOrNull()?.getAddressLine(0)

    @Suppress("DEPRECATION")
    private suspend fun geocode(
        address: String? = null,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): List<Address> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext emptyList()
        val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(10_000L) {
                    suspendCancellableCoroutine { continuation ->
                        val listener = object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(emptyList())
                            }
                        }
                        if (address != null) {
                            geocoder.getFromLocationName(address, 1, listener)
                        } else {
                            geocoder.getFromLocation(latitude, longitude, 1, listener)
                        }
                    }
                } ?: emptyList()
            } else {
                if (address != null) {
                    geocoder.getFromLocationName(address, 1).orEmpty()
                } else {
                    geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
                }
            }
        } catch (_: IOException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
