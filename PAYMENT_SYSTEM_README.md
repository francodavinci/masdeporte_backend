# Sistema de Pagos y Reservas Automáticas - Agendalo

## Descripción General

El sistema implementa un flujo completo de pagos con Mercado Pago que automáticamente crea reservas (appointments) cuando el pago es exitoso.

## Flujo del Sistema

### 1. Creación de Preferencia de Pago

**Endpoint:** `POST /api/mercadopago/preferences`

**Request Body:**
```json
{
  "title": "Servicio de Peluquería",
  "description": "Corte y peinado",
  "amount": 5000.00,
  "quantity": 1,
  "currency": "ARS",
  "serviceId": 1,
  "userId": 1,
  "startTime": "2024-01-15T14:00:00",
  "notes": "Cliente preferencia corte corto",
  "userEmail": "cliente@ejemplo.com"
}
```

**Proceso:**
1. Se crea la preferencia en Mercado Pago
2. Se guarda la información de la reserva en la tabla `payment_preference`
3. Se retorna la preferencia con el ID de Mercado Pago

### 2. Procesamiento de Webhook

**Endpoint:** `POST /api/mercadopago/webhook`

**Proceso:**
1. Mercado Pago envía notificación cuando el pago cambia de estado
2. El sistema verifica si el pago fue aprobado (`status: "approved"`)
3. Si es aprobado, busca la preferencia asociada
4. Crea automáticamente la reserva en la tabla `appointment` con estado `CONFIRMED`
5. Actualiza el estado de la preferencia a `approved`

### 3. Verificación de Estado de Pago

**Endpoint:** `GET /api/mercadopago/payment/{paymentId}/status`

Permite verificar el estado de un pago específico desde el frontend.

### 4. Consulta de Preferencias de Usuario

**Endpoint:** `GET /api/mercadopago/preferences`

Retorna todas las preferencias de pago del usuario autenticado.

## Endpoints Adicionales para el Frontend

### 5. Obtener Preferencia Específica

**Endpoint:** `GET /api/mercadopago/preferences/{preferenceId}`

Retorna una preferencia específica con toda su información detallada.

### 6. Reservas Pendientes de Pago

**Endpoint:** `GET /api/mercadopago/appointments/pending-payment`

Retorna todas las reservas que están pendientes de pago del usuario autenticado.

### 7. Cancelar Preferencia de Pago

**Endpoint:** `POST /api/mercadopago/preferences/{preferenceId}/cancel`

Permite al usuario cancelar una preferencia de pago pendiente.

### 8. Estadísticas de Pagos

**Endpoint:** `GET /api/mercadopago/statistics`

Retorna estadísticas de pagos del usuario:
```json
{
  "totalPreferences": 10,
  "pendingCount": 2,
  "approvedCount": 7,
  "cancelledCount": 1,
  "rejectedCount": 0,
  "totalEarnings": 35000.00,
  "recentCount": 3
}
```

## Entidades del Sistema

### PaymentPreference
Almacena temporalmente la información de la reserva mientras se procesa el pago:
- `preferenceId`: ID de Mercado Pago
- `service`: Servicio solicitado
- `user`: Usuario que hace la reserva
- `company`: Empresa que ofrece el servicio
- `startTime`: Fecha y hora de inicio
- `notes`: Notas adicionales
- `amount`: Monto del pago
- `status`: Estado del pago (pending, approved, rejected, cancelled)

### Appointment
Reserva creada automáticamente después del pago exitoso:
- Estado inicial: `CONFIRMED` (confirmado por pago exitoso)
- Se calcula automáticamente `endTime` basado en la duración del servicio

## Funcionalidades del Frontend

### Dashboard de Usuario
- Ver todas las preferencias de pago
- Ver reservas pendientes de pago
- Cancelar preferencias pendientes
- Ver estadísticas de pagos

### Flujo de Reserva con Pago
1. Usuario selecciona servicio y horario
2. Se crea preferencia de pago
3. Usuario es redirigido a Mercado Pago
4. Después del pago, se crea automáticamente la reserva
5. Usuario puede ver el estado en tiempo real

### Notificaciones
- El frontend puede consultar el estado del pago en tiempo real
- Mostrar confirmación cuando la reserva se crea automáticamente
- Alertas para pagos fallidos o cancelados

## Configuración

### Variables de Entorno
```properties
# Mercado Pago
mercadopago.client.id=tu_client_id
mercadopago.client.secret=tu_client_secret
mercadopago.redirect.uri=tu_redirect_uri
mercadopago.notification-url=https://tu-dominio.com/api/mercadopago/webhook

# URLs de retorno
mercadopago.back-urls.success=https://tu-dominio.com/payment-success
mercadopago.back-urls.failure=https://tu-dominio.com/payment-failure
mercadopago.back-urls.pending=https://tu-dominio.com/payment-pending
```

## Seguridad

- Los tokens de acceso se encriptan antes de almacenarse
- Se valida la autenticación en todos los endpoints
- Los webhooks se procesan de forma segura con manejo de errores
- Verificación de permisos para cancelar preferencias

## Manejo de Errores

El sistema incluye manejo robusto de errores:
- Validación de datos de entrada
- Manejo de errores de comunicación con Mercado Pago
- Rollback automático en caso de fallos en la creación de reservas
- Logs detallados para debugging
- Respuestas estandarizadas con ApiResponse

## Consideraciones de Producción

1. **Webhooks**: Asegúrate de que la URL del webhook sea accesible públicamente
2. **SSL**: Usa HTTPS en producción para todas las comunicaciones
3. **Rate Limiting**: El sistema incluye rate limiting para prevenir abusos
4. **Logging**: Implementa logging detallado para monitoreo
5. **Backup**: Realiza backups regulares de las tablas de pagos y reservas
6. **Monitoreo**: Implementa alertas para webhooks fallidos

## Testing

Para probar el sistema:
1. Usa las credenciales de sandbox de Mercado Pago
2. Simula webhooks usando las herramientas de testing de Mercado Pago
3. Verifica que las reservas se creen correctamente después de pagos exitosos
4. Prueba casos de error (pagos rechazados, cancelados, etc.)
5. Prueba la cancelación de preferencias pendientes
6. Verifica las estadísticas de pagos 