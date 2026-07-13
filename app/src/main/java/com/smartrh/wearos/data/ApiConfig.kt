package com.smartrh.wearos.data

object ApiConfig {
    /*
        EMULADOR WEAR OS:
        Usa siempre 10.0.2.2 aunque estés en tu casa, universidad u otra red.
        El emulador usa 10.0.2.2 para conectarse al localhost de tu Mac.

        BACKEND EN TU MAC:
        npm run dev
        http://localhost:4000

        RELOJ FÍSICO REAL:
        Solo si después pruebas en un reloj físico, cambia BASE_URL
        por la IP actual de tu Mac.

        Para ver tu IP actual:
        ipconfig getifaddr en0

        Ejemplo:
        const val BASE_URL = "http://192.168.3.73:4000"
    */

    const val BASE_URL = "http://10.0.2.2:4000"

    const val DEFAULT_DEVICE_ID = "WATCH-001"
    const val DEFAULT_DEVICE_NAME = "SMART RH Watch"
    const val DEFAULT_MODEL = "Wear OS Large Round"
    const val DEFAULT_PLATFORM = "Wear OS"

    /*
        Se deja en 0 porque ahora el usuario real se obtiene desde backend
        mediante el device_id vinculado.
        WATCH-001 ya está asociado al usuario 22 en MySQL.
    */
    const val DEFAULT_USER_ID = 0
}