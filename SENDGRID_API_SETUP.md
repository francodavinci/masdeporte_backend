# Configuración SendGrid API para Railway

## ✅ Problema Resuelto

Railway deshabilitó SMTP en el plan básico, por lo que implementamos SendGrid API HTTPS que es más confiable y rápido.

## 🔧 Cambios Implementados

### 1. **Dependencia Agregada**
```xml
<!-- SendGrid Java SDK -->
<dependency>
    <groupId>com.sendgrid</groupId>
    <artifactId>sendgrid-java</artifactId>
    <version>4.10.2</version>
</dependency>
```

### 2. **Nuevos Archivos Creados**
- `SendGridConfig.java` - Configuración de SendGrid API
- `SendGridEmailServiceImpl.java` - Implementación usando API HTTPS
- `application.properties` - Configuración para desarrollo local

### 3. **Configuración Actualizada**
- `application-prod.properties` - Configuración optimizada para Railway
- `EmailServiceImpl.java` - Marcado como secundario (solo desarrollo)
- `EmailConfig.java` - Condicional para desarrollo local

## 📋 Variables de Entorno Requeridas en Railway

```bash
# SendGrid API (OBLIGATORIO)
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Opcionales (tienen valores por defecto)
SENDGRID_FROM_EMAIL=no-reply@agendalo.com
SENDGRID_FROM_NAME=Agendalo

# Otras variables existentes...
DB_HOST=tu_host
DB_PORT=3306
DB_NAME=tu_database
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_password
PORT=8080
# ... resto de variables
```

## 🚀 Ventajas de SendGrid API vs SMTP

### ✅ **Ventajas:**
- **Más rápido**: No hay timeouts de conexión SMTP
- **Más confiable**: API REST con mejor manejo de errores
- **Mejor logging**: Status codes HTTP específicos
- **Retry automático**: Configurado con backoff exponencial
- **Compatible con Railway**: No bloqueado en plan básico

### 📊 **Métricas Mejoradas:**
- Tiempo de envío en milisegundos
- Status codes HTTP (200, 400, 401, etc.)
- Número de intentos realizados
- Errores específicos de la API

## 🔄 Cómo Funciona

### **En Producción (Railway):**
1. Se detecta `SENDGRID_API_KEY` en variables de entorno
2. Se activa `SendGridEmailServiceImpl` como servicio principal
3. Se usa API HTTPS para enviar emails
4. Retry automático con backoff exponencial

### **En Desarrollo Local:**
1. Si no hay `SENDGRID_API_KEY`, usa `EmailServiceImpl` (SMTP)
2. Puedes configurar SMTP local o usar SendGrid API
3. Configuración flexible en `application.properties`

## 📝 Configuración en Railway

### 1. **Obtener API Key de SendGrid:**
1. Ve a [SendGrid Dashboard](https://app.sendgrid.com/)
2. Settings → API Keys
3. Create API Key
4. Selecciona "Full Access" o "Restricted Access"
5. Copia la API Key (empieza con `SG.`)

### 2. **Configurar en Railway:**
1. Ve a tu proyecto en Railway
2. Variables → Add Variable
3. Nombre: `SENDGRID_API_KEY`
4. Valor: `SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 3. **Variables Opcionales:**
```bash
SENDGRID_FROM_EMAIL=tu-email@tudominio.com
SENDGRID_FROM_NAME=Tu Nombre de Empresa
```

## 🧪 Testing

### **Logs a Verificar:**
```
INFO - Preparando email de bienvenida al usuario: usuario@email.com (SendGrid API)
INFO - Intento 1 de 3 para enviar bienvenida a: usuario@email.com (SendGrid API)
INFO - Email bienvenida enviado exitosamente a usuario@email.com en 245ms (intento 1) - Status: 202
```

### **Status Codes Esperados:**
- `202` - Email aceptado y enviado
- `400` - Error en la solicitud (revisar formato)
- `401` - API Key inválida
- `403` - Sin permisos para enviar

## 🔧 Troubleshooting

### **Error: "SendGrid API key is required"**
- Verifica que `SENDGRID_API_KEY` esté configurada en Railway
- Asegúrate de que la API Key sea válida

### **Error: "401 Unauthorized"**
- La API Key es inválida o expiró
- Genera una nueva API Key en SendGrid

### **Error: "403 Forbidden"**
- La API Key no tiene permisos de envío
- Verifica los permisos en SendGrid Dashboard

### **Emails no llegan:**
- Revisa la carpeta de spam
- Verifica que el dominio esté verificado en SendGrid
- Revisa los logs para errores específicos

## 📈 Monitoreo

### **Logs Mejorados:**
- ⏱️ Tiempo de envío exacto
- 🔄 Número de intentos
- 📊 Status codes HTTP
- ❌ Errores específicos de API

### **Métricas de SendGrid:**
- Ve a SendGrid Dashboard → Activity
- Monitorea entregas, bounces, spam reports
- Configura webhooks para eventos en tiempo real

## 🔄 Rollback (si es necesario)

Si necesitas volver a SMTP temporalmente:

1. **Comenta la API Key en Railway:**
   ```bash
   # SENDGRID_API_KEY=SG.xxx...
   ```

2. **Configura SMTP en application-prod.properties:**
   ```properties
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=tu_email@gmail.com
   spring.mail.password=tu_app_password
   ```

3. **Redeploy** la aplicación

## 🎯 Próximos Pasos

1. ✅ Configura `SENDGRID_API_KEY` en Railway
2. ✅ Despliega los cambios
3. ✅ Verifica logs de envío exitoso
4. ✅ Monitorea métricas en SendGrid Dashboard
5. 🔄 Considera configurar webhooks para mejor monitoreo
