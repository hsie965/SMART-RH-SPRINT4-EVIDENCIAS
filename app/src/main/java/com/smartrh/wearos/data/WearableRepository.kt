package com.smartrh.wearos.data

import com.smartrh.wearos.model.AccelerometerPayload
import com.smartrh.wearos.model.LocationPayload
import com.smartrh.wearos.model.OperationResult
import com.smartrh.wearos.model.WearableDeviceRequest
import com.smartrh.wearos.model.WearableEventRequest
import com.smartrh.wearos.model.WearableNotification
import com.smartrh.wearos.model.WearableSensorRequest

class WearableRepository(
    private val api: WearableApi = ApiClient.api
) {
    suspend fun registrarDispositivo(): OperationResult {
        return safeCall {
            api.registrarDispositivo(
                WearableDeviceRequest(
                    deviceId = ApiConfig.DEFAULT_DEVICE_ID,
                    usuarioId = ApiConfig.DEFAULT_USER_ID,
                    nombreDispositivo = ApiConfig.DEFAULT_DEVICE_NAME,
                    modelo = "Wear OS Emulator"
                )
            )
        }
    }

    suspend fun enviarEvento(
        tipoEvento: String,
        location: LocationPayload?,
        accelerometer: AccelerometerPayload,
        battery: Int
    ): OperationResult {
        return safeCall {
            api.enviarEvento(
                WearableEventRequest(
                    deviceId = ApiConfig.DEFAULT_DEVICE_ID,
                    usuarioId = ApiConfig.DEFAULT_USER_ID,
                    tipoEvento = tipoEvento,
                    latitud = location?.latitude,
                    longitud = location?.longitude,
                    movimiento = accelerometer.movement,
                    bateria = battery,
                    acelerometroX = accelerometer.x,
                    acelerometroY = accelerometer.y,
                    acelerometroZ = accelerometer.z
                )
            )
        }
    }

    suspend fun enviarSensores(
        location: LocationPayload?,
        accelerometer: AccelerometerPayload,
        battery: Int
    ): OperationResult {
        return safeCall {
            api.enviarSensores(
                WearableSensorRequest(
                    deviceId = ApiConfig.DEFAULT_DEVICE_ID,
                    usuarioId = ApiConfig.DEFAULT_USER_ID,
                    latitud = location?.latitude,
                    longitud = location?.longitude,
                    bateria = battery,
                    acelerometroX = accelerometer.x,
                    acelerometroY = accelerometer.y,
                    acelerometroZ = accelerometer.z,
                    movimiento = accelerometer.movement
                )
            )
        }
    }

    suspend fun obtenerNotificaciones(): Result<List<WearableNotification>> {
        return try {
            val response = api.obtenerNotificaciones(ApiConfig.DEFAULT_DEVICE_ID)

            if (response.ok) {
                Result.success(response.notificaciones)
            } else {
                Result.failure(Exception(response.message ?: "No se pudieron obtener notificaciones."))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun safeCall(block: suspend () -> com.smartrh.wearos.model.ApiResponse): OperationResult {
        return try {
            val response = block()

            OperationResult(
                success = response.ok,
                message = response.message ?: if (response.ok) "Operación realizada correctamente." else "Operación no completada."
            )
        } catch (error: Exception) {
            OperationResult(
                success = false,
                message = error.message ?: "Error de conexión con SMART RH Backend."
            )
        }
    }
}
