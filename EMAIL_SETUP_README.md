# Configuración del Servicio de Email

## Descripción
Se ha implementado un sistema completo de envío de emails para notificaciones en Agendalo. El sistema incluye:

- ✅ **Email de bienvenida** al usuario al registrarse
- ✅ Confirmación de turno al usuario
- ✅ Notificación de nuevo turno a la empresa
- ✅ Notificación de cancelación al usuario
- ✅ Notificación de cancelación a la empresa

## Configuración Requerida

### 1. Variables de Entorno
Agregar las siguientes variables de entorno o configurar en `application.properties`:

```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME:}
spring.mail.password=${EMAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### 2. Configuración de Gmail (Recomendado)

#### Opción A: Usar App Password (Recomendado)
1. Habilitar la verificación en 2 pasos en tu cuenta de Gmail
2. Generar una "App Password" específica para la aplicación
3. Usar la App Password como `EMAIL_PASSWORD`

#### Opción B: Usar OAuth2
1. Crear un proyecto en Google Cloud Console
2. Habilitar Gmail API
3. Crear credenciales OAuth2
4. Configurar el flujo de autenticación

### 3. Otras Opciones de SMTP

#### Outlook/Hotmail
```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
```

#### Yahoo
```properties
spring.mail.host=smtp.mail.yahoo.com
spring.mail.port=587
```

#### Servidor SMTP Personalizado
```properties
spring.mail.host=tu-servidor-smtp.com
spring.mail.port=587
spring.mail.username=tu-usuario
spring.mail.password=tu-password
```

## Plantillas de Email

Las plantillas están ubicadas en `src/main/resources/templates/email/`:

- `welcome-user.html` - **Email de bienvenida al usuario**
- `appointment-confirmation-user.html` - Confirmación de turno al usuario
- `appointment-notification-company.html` - Notificación de turno a la empresa
- `appointment-cancellation-user.html` - Cancelación de turno al usuario
- `appointment-cancellation-company.html` - Cancelación de turno a la empresa

### Personalización de Plantillas
Las plantillas usan Thymeleaf y pueden ser personalizadas fácilmente. Variables disponibles:

**Para emails de bienvenida:**
- `user` - Objeto completo del usuario
- `user.name` - Nombre del usuario
- `user.email` - Email del usuario
- `user.role` - Rol del usuario

**Para emails de turnos:**
- `appointment` - Objeto completo del turno
- `appointment.user` - Información del usuario
- `appointment.company` - Información de la empresa
- `appointment.service` - Información del servicio

## Uso del Servicio

### Envío Automático
Los emails se envían automáticamente cuando:
- **Se registra un nuevo usuario** (email de bienvenida)
- Se crea un nuevo turno (confirmación al usuario + notificación a la empresa)
- Se cancela un turno (notificación a ambos)

### Envío Manual
```java
@Autowired
private EmailService emailService;

// Enviar email de bienvenida
emailService.sendWelcomeEmail(user);

// Enviar confirmación de turno al usuario
emailService.sendAppointmentConfirmationToUser(appointment);

// Enviar notificación de turno a la empresa
emailService.sendAppointmentNotificationToCompany(appointment);
```

## Logs y Monitoreo

El servicio incluye logging detallado:
- ✅ Confirmación de envío exitoso
- ❌ Errores de envío (no interrumpen el flujo principal)
- 📊 Información de debugging

## Consideraciones de Seguridad

1. **Nunca hardcodear credenciales** en el código
2. **Usar variables de entorno** para credenciales sensibles
3. **Configurar App Passwords** en lugar de contraseñas principales
4. **Revisar logs** regularmente para detectar problemas

## Testing

### Modo de Desarrollo
Para testing local, puedes usar servicios como:
- **MailHog** - Servidor SMTP local para testing
- **Mailtrap** - Servicio de testing de emails
- **Gmail Sandbox** - Modo de prueba de Gmail

### Configuración para Testing
```properties
# Para MailHog local
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.username=
spring.mail.password=
```

## Troubleshooting

### Error: "Authentication failed"
- Verificar credenciales
- Usar App Password en lugar de contraseña principal
- Verificar que la verificación en 2 pasos esté habilitada

### Error: "Connection refused"
- Verificar configuración de host y puerto
- Verificar conectividad de red
- Verificar configuración de firewall

### Emails no llegan
- Revisar carpeta de spam
- Verificar logs de la aplicación
- Verificar configuración SMTP

## Próximas Mejoras

- [ ] Configuración de templates por empresa
- [ ] Soporte para múltiples idiomas
- [ ] Cola de emails para mejor rendimiento
- [ ] Métricas de entrega de emails
- [ ] Plantillas personalizables desde el admin
