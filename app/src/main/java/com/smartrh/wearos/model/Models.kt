package com.smartrh.wearos.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val ok: Boolean = false,
    val message: String? = null
)

data class WearableDeviceRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("nombre_dispositivo") val nombreDispositivo: String,
    val modelo: String,
    val estado: String = "ACTIVO"
)

data class WearableEventRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("tipo_evento") val tipoEvento: String,
    val latitud: Double?,
    val longitud: Double?,
    val movimiento: Double,
    val bateria: Int,
    @SerializedName("acelerometro_x") val acelerometroX: Float,
    @SerializedName("acelerometro_y") val acelerometroY: Float,
    @SerializedName("acelerometro_z") val acelerometroZ: Float
)

data class WearableSensorRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("usuario_id") val usuarioId: Int,
    val latitud: Double?,
    val longitud: Double?,
    val bateria: Int,
    @SerializedName("acelerometro_x") val acelerometroX: Float,
    @SerializedName("acelerometro_y") val acelerometroY: Float,
    @SerializedName("acelerometro_z") val acelerometroZ: Float,
    val movimiento: Double
)

data class WearableNotification(
    val id: Int,
    @SerializedName("device_id") val deviceId: String? = null,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean = false,
    @SerializedName("fecha_envio") val fechaEnvio: String? = null
)

data class NotificationsResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val notificaciones: List<WearableNotification> = emptyList()
)

data class AccelerometerPayload(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val movement: Double = 0.0
)

data class LocationPayload(
    val latitude: Double?,
    val longitude: Double?
)

data class OperationResult(
    val success: Boolean,
    val message: String
)
