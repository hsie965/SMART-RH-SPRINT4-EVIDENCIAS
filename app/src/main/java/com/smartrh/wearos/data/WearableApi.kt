package com.smartrh.wearos.data

import com.smartrh.wearos.model.ApiResponse
import com.smartrh.wearos.model.NotificationsResponse
import com.smartrh.wearos.model.WearableDeviceRequest
import com.smartrh.wearos.model.WearableEventRequest
import com.smartrh.wearos.model.WearableSensorRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface WearableApi {
    @POST("wearables/dispositivos")
    suspend fun registrarDispositivo(@Body request: WearableDeviceRequest): ApiResponse

    @POST("wearables/eventos")
    suspend fun enviarEvento(@Body request: WearableEventRequest): ApiResponse

    @POST("wearables/sensores")
    suspend fun enviarSensores(@Body request: WearableSensorRequest): ApiResponse

    @GET("wearables/notificaciones/{deviceId}")
    suspend fun obtenerNotificaciones(@Path("deviceId") deviceId: String): NotificationsResponse

    @PATCH("wearables/notificaciones/{id}/leida")
    suspend fun marcarNotificacionLeida(@Path("id") id: Int): ApiResponse
}
