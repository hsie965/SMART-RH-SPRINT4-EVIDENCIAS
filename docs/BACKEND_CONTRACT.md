# Contrato esperado para SMART RH Backend — Wearables

Este archivo define las rutas que la app Wear OS espera consumir.

## 1. Registrar dispositivo

```http
POST /wearables/dispositivos
```

Body:

```json
{
  "device_id": "WATCH-001",
  "usuario_id": 1,
  "nombre_dispositivo": "SMART RH Watch 001",
  "modelo": "Wear OS Emulator",
  "estado": "ACTIVO"
}
```

Respuesta esperada:

```json
{
  "ok": true,
  "message": "Dispositivo registrado correctamente."
}
```

## 2. Enviar evento de asistencia

```http
POST /wearables/eventos
```

Body:

```json
{
  "device_id": "WATCH-001",
  "usuario_id": 1,
  "tipo_evento": "ENTRADA",
  "latitud": 19.3404,
  "longitud": -99.4785,
  "movimiento": 9.81,
  "bateria": 85,
  "acelerometro_x": 0.1,
  "acelerometro_y": 0.2,
  "acelerometro_z": 9.7
}
```

Tipos sugeridos:

```txt
ENTRADA
SALIDA
ALERTA
```

## 3. Enviar sensores

```http
POST /wearables/sensores
```

Body:

```json
{
  "device_id": "WATCH-001",
  "usuario_id": 1,
  "latitud": 19.3404,
  "longitud": -99.4785,
  "bateria": 85,
  "acelerometro_x": 0.1,
  "acelerometro_y": 0.2,
  "acelerometro_z": 9.7,
  "movimiento": 9.81
}
```

## 4. Consultar notificaciones

```http
GET /wearables/notificaciones/WATCH-001
```

Respuesta esperada:

```json
{
  "ok": true,
  "notificaciones": [
    {
      "id": 1,
      "device_id": "WATCH-001",
      "titulo": "Recordatorio",
      "mensaje": "No olvides marcar tu salida.",
      "leida": false,
      "fecha_envio": "2026-07-10T10:00:00.000Z"
    }
  ]
}
```

## 5. Marcar notificación como leída

```http
PATCH /wearables/notificaciones/1/leida
```

Respuesta esperada:

```json
{
  "ok": true,
  "message": "Notificación marcada como leída."
}
```
