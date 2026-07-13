# Plan de desarrollo — SMART RH Wear OS

## Fase 1 — Proyecto Wear OS

- Crear carpeta independiente `rrhh-wear-os`.
- Abrirla en Android Studio.
- Ejecutarla en un emulador Wear OS.
- Validar las cuatro pantallas principales.

## Fase 2 — Backend

- Crear módulo `wearables` en `rrhh-backend`.
- Crear tablas en MySQL.
- Crear endpoints REST.
- Validar con Postman o curl.

## Fase 3 — Portal web

- Agregar módulo `Dispositivos Wearables` en SMART RH Web Portal.
- Mostrar dispositivos registrados.
- Mostrar eventos enviados por reloj.
- Mostrar sensores.
- Enviar notificaciones.

## Fase 4 — Integración

- Correr backend en puerto 4000.
- Correr Wear OS en emulador.
- Enviar evento de entrada.
- Confirmar que aparece en MySQL.
- Confirmar que aparece en el portal.
