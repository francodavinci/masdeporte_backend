# Configuración de Email para Railway

## Problemas Identificados y Soluciones

### 1. Timeouts en Railway
**Problema**: Railway tiene timeouts más largos que el entorno local, especialmente para operaciones SMTP.

**Solución Implementada**:
- Aumenté los timeouts de 10s a 30-60s
- Agregué connection pooling
- Implementé retry logic con backoff exponencial

### 2. Configuración de SendGrid
**Problema**: La configuración estaba mezclada entre Gmail y SendGrid.

**Solución Implementada**:
- Configuración específica para SendGrid en `application-prod.properties`
- Configuración dinámica en `EmailConfig.java` que detecta el proveedor

## Variables de Entorno Requeridas en Railway

```bash
# SendGrid Configuration
SENDGRID_API_KEY=tu_api_key_de_sendgrid

# Database (ya configurado)
DB_HOST=tu_host
DB_PORT=3306
DB_NAME=tu_database
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_password

# Server
PORT=8080

# File Upload
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=10MB
FILES_UPLOAD_DIR=/tmp/uploads

# Google OAuth
GOOGLE_OAUTH_CLIENT_ID=tu_client_id

# Google Calendar
CALENDAR_CLIENT_ID=tu_calendar_client_id
CALENDAR_CLIENT_SECRET=tu_calendar_client_secret
CALENDAR_REDIRECT_URI=tu_calendar_redirect_uri
CALENDAR_SCOPES=https://www.googleapis.com/auth/calendar

# MercadoPago
MP_BASE_URL=https://api.mercadopago.com
MP_TOKEN_PATH=/oauth/token
MP_PREFERENCES_PATH=/checkout/preferences
MP_CALLBACK_URL=tu_callback_url
MP_NOTIFICATION_URL=tu_notification_url
MP_CLIENT_ID=tu_mp_client_id
MP_CLIENT_SECRET=tu_mp_client_secret
MP_PAYMENT_CALLBACK_URL=tu_payment_callback_url

# Encryption
ENCRYPTION_KEY=tu_encryption_key
```

## Configuraciones Optimizadas para Railway

### Timeouts Aumentados
```properties
# Timeouts optimizados para Railway
spring.mail.properties.mail.smtp.connectiontimeout=30000
spring.mail.properties.mail.smtp.timeout=60000
spring.mail.properties.mail.smtp.writetimeout=30000
```

### Connection Pooling
```properties
# Connection pooling para mejor rendimiento
spring.mail.properties.mail.smtp.connectionpool=true
spring.mail.properties.mail.smtp.connectionpooltimeout=10000
```

### Retry Logic
- 3 intentos máximo
- Backoff exponencial (2s, 4s, 6s)
- Logging detallado de cada intento

## Troubleshooting

### Si sigues teniendo timeouts:

1. **Verifica la API Key de SendGrid**:
   ```bash
   # En Railway, verifica que SENDGRID_API_KEY esté configurada correctamente
   ```

2. **Revisa los logs**:
   ```bash
   # Los logs ahora muestran:
   # - Tiempo de envío
   # - Número de intento
   # - Errores específicos
   ```

3. **Alternativas si SendGrid falla**:
   - Gmail con App Password
   - Mailgun
   - Amazon SES

### Configuración de Gmail (Alternativa)
Si quieres cambiar a Gmail, actualiza estas variables en Railway:
```bash
# Cambiar en Railway
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu_email@gmail.com
spring.mail.password=tu_app_password
```

## Monitoreo

Los logs ahora incluyen:
- ✅ Tiempo de envío en milisegundos
- 🔄 Número de intento
- ❌ Errores específicos con stack trace
- 📊 Estadísticas de retry

## Próximos Pasos

1. Despliega los cambios en Railway
2. Monitorea los logs para verificar que los timeouts se resolvieron
3. Si persisten problemas, considera cambiar a un servicio más confiable como Mailgun
