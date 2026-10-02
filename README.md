# GrabaTexto Android

Grabadora Android diseñada para seguir grabando al minimizar la aplicación o bloquear/apagar la pantalla.

## Arquitectura
- Kotlin nativo
- Android Foreground Service de tipo microphone
- Grabación AAC/M4A en almacenamiento privado
- Notificación permanente durante la grabación
- Acción Finalizar desde la notificación
- Historial local
- GitHub Actions genera un APK de prueba en cada push a main

## Flujo
1. Conceder permiso de micrófono/notificaciones.
2. Pulsar GRABAR.
3. Minimizar o bloquear la pantalla: el servicio continúa.
4. Finalizar desde la app o notificación.
5. El audio queda guardado para transcripción.

## Siguiente fase
Transcripción posterior, texto editable y exportación/compartir.


Build APK automático activado.
