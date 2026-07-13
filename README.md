# SMART RH Wear OS

Proyecto independiente para **Wear OS** que se conecta con el backend de SMART RH mediante API REST.

## Carpeta que debes abrir en Android Studio

Abre la carpeta raíz:

```txt
rrhh-wear-os
```

No abras `app` directamente.

## Correcciones incluidas

Esta versión ya viene corregida para evitar el error de `Android resource linking failed`:

- Se eliminó `android:roundIcon="@drawable/ic_launcher_round"`, porque ese recurso no existía.
- Se agregó `network_security_config.xml` para permitir conexión HTTP local.
- Se agregó `android:usesCleartextTraffic="true"`.
- Se corrigió el tema `Theme.SmartRhWear`.
- Se agregó `ACCESS_NETWORK_STATE`.
- Se validó que existan las carpetas `res/xml`, `res/values` y `res/drawable`.
- Se dejó el backend configurado para emulador Android/Wear OS con `http://10.0.2.2:4000`.

## Requisitos

- Android Studio instalado.
- Android SDK configurado.
- Emulador Wear OS creado, por ejemplo: `Wear OS Large Round`.
- Backend SMART RH corriendo en puerto 4000.

## Ejecutar

1. Abre `rrhh-wear-os` en Android Studio.
2. Espera a que termine `Gradle Sync`.
3. Selecciona el emulador `Wear OS Large Round`.
4. Selecciona configuración `app`.
5. Presiona `Run`.

## Backend

En VS Code corre:

```bash
cd rrhh-backend
npm run dev
```

Debe estar activo en:

```txt
http://localhost:4000
```

Desde el emulador Wear OS se consume como:

```txt
http://10.0.2.2:4000
```

## Pantallas incluidas

- Inicio / Dashboard
- Asistencia
- Sensores
- Avisos / Notificaciones

## Sensores contemplados

- Ubicación GPS
- Acelerómetro
- Batería

## Endpoints esperados

La app queda preparada para consumir:

```txt
POST  /wearables/dispositivos
POST  /wearables/eventos
POST  /wearables/sensores
GET   /wearables/notificaciones/:deviceId
PATCH /wearables/notificaciones/:id/leida
```

El siguiente paso es implementar el módulo `/wearables` en el backend de SMART RH.
