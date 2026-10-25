# Probadores Rincón Sur

App Android para el caso de la Tienda Rincón Sur (DSY1105 - Desarrollo de Aplicaciones Móviles, Duoc UC).

La idea es que el cliente sepa cuánto tiene que esperar para un probador y que el personal pueda manejar la fila y ver qué probadores están libres u ocupados.

## Integrantes

- Stiven Soto

## Lo que hace la app

- Login de colaboradores con RUT y clave (asistente, vendedor y jefatura)
- Panel con los 8 probadores y su estado (libre, ocupado, ordenamiento, fuera de servicio)
- Registrar turnos en la fila y llamar al siguiente
- Registrar la salida del cliente y las prendas que devuelve
- Registrar abandonos de la fila
- Calcular el tiempo estimado de espera
- Pantalla para clientes con los probadores libres, la gente en la fila y el tiempo estimado
- Aviso cuando un probador lleva mucho rato ocupado
- Resumen del día para la jefatura (abandonos, promedios y venta perdida)
- Jefatura puede cambiar los parámetros y agregar colaboradores

Recursos nativos: cámara (foto cuando un probador queda fuera de servicio) y notificaciones.

## Tecnologías

Kotlin, Jetpack Compose, Material 3, MVVM, Room, Navigation Compose.

## Cómo ejecutar

1. Clonar el repo
2. Abrir la carpeta en Android Studio
3. Esperar que sincronice Gradle (si pide JVM, usar la 21)
4. Ejecutar en un emulador con Android 8 o superior

Usuarios de prueba (clave 1234 para todos):

- Jefatura: 12.984.561-9 (Patricia Elena Morales Castillo)
- Asistente: 16.842.311-K (Camila Andrea Fuentes Rojas)
- Vendedor: 17.550.982-8 (Javiera Paz Muñoz Contreras)

## Trello

(link del tablero)
