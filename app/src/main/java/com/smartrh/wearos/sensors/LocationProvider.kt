package com.smartrh.wearos.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.smartrh.wearos.model.LocationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationProvider(private val context: Context) {

    /*
        Ubicación de respaldo para pruebas en emulador.
        El emulador Wear OS muchas veces no entrega GPS real,
        por eso antes se guardaban latitud y longitud como NULL.

        Puedes cambiar estos valores por la ubicación real de tu escuela,
        oficina o zona de pruebas.
    */
    private val fallbackLatitude = 19.3404148
    private val fallbackLongitude = -99.4785947

    suspend fun getCurrentLocation(): LocationPayload {
        return withContext(Dispatchers.IO) {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                return@withContext LocationPayload(
                    latitude = fallbackLatitude,
                    longitude = fallbackLongitude
                )
            }

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            val bestLocation = providers
                .mapNotNull { provider ->
                    try {
                        if (locationManager.isProviderEnabled(provider)) {
                            locationManager.getLastKnownLocation(provider)
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                .maxByOrNull { location: Location -> location.time }

            if (bestLocation != null) {
                LocationPayload(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude
                )
            } else {
                LocationPayload(
                    latitude = fallbackLatitude,
                    longitude = fallbackLongitude
                )
            }
        }
    }
}