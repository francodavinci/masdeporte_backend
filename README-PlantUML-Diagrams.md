# Diagramas C4 del Sistema Agendalo - PlantUML

Este directorio contiene los diagramas C4 del sistema Agendalo creados en PlantUML.

## Archivos de Diagramas

### 📁 Archivos Individuales por Nivel:
- **`agendalo-c4-nivel1-contexto.puml`** - Diagrama de Contexto (Nivel 1)
- **`agendalo-c4-nivel2-contenedores.puml`** - Diagrama de Contenedores (Nivel 2)
- **`agendalo-c4-nivel3-componentes.puml`** - Diagrama de Componentes (Nivel 3)
- **`agendalo-c4-nivel4-entidades.puml`** - Diagrama de Entidades (Nivel 4)

### 📁 Archivo Completo:
- **`agendalo-c4-plantuml.puml`** - Todos los niveles en un solo archivo

## Cómo Visualizar los Diagramas

### Opción 1: PlantUML Online
1. Ve a [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)
2. Copia el contenido de cualquier archivo `.puml`
3. Pega en el editor
4. Los diagramas se generarán automáticamente

### Opción 2: Extensión de VS Code
1. Instala la extensión "PlantUML" en VS Code
2. Abre cualquier archivo `.puml`
3. Usa `Ctrl+Shift+P` y busca "PlantUML: Preview Current Diagram"

### Opción 3: IntelliJ IDEA
1. Instala el plugin "PlantUML integration"
2. Abre cualquier archivo `.puml`
3. Usa `Alt+D` para generar el diagrama

### Opción 4: Herramientas de Línea de Comandos
```bash
# Instalar PlantUML (requiere Java)
# Descargar plantuml.jar desde https://plantuml.com/download

# Generar imagen PNG
java -jar plantuml.jar agendalo-c4-nivel1-contexto.puml

# Generar imagen SVG
java -jar plantuml.jar -tsvg agendalo-c4-nivel1-contexto.puml

# Generar PDF
java -jar plantuml.jar -tpdf agendalo-c4-nivel1-contexto.puml
```

## Descripción de los Niveles C4

### 🎯 Nivel 1: Diagrama de Contexto
- **Propósito**: Vista de alto nivel del sistema y sus interacciones externas
- **Elementos**: Usuarios, Sistema Agendalo, Servicios Externos
- **Relaciones**: Flujos de datos entre actores y sistemas

### 🏗️ Nivel 2: Diagrama de Contenedores
- **Propósito**: Arquitectura interna del sistema mostrando contenedores principales
- **Elementos**: Frontend, Backend API, Servicios, Base de Datos
- **Relaciones**: Comunicación entre contenedores

### 🔧 Nivel 3: Diagrama de Componentes
- **Propósito**: Estructura interna del backend API
- **Elementos**: Controllers, Services, Repositories, Configuraciones
- **Relaciones**: Dependencias entre componentes

### 📊 Nivel 4: Diagrama de Entidades
- **Propósito**: Modelo de datos del sistema
- **Elementos**: Entidades de dominio y sus atributos
- **Relaciones**: Relaciones entre entidades (1:1, 1:N, N:M)

## Características del Sistema Agendalo

### 🏢 **Funcionalidades Principales:**
- Gestión de usuarios (clientes y propietarios)
- Gestión de empresas y servicios
- Sistema de reservas y citas
- Procesamiento de pagos con MercadoPago
- Sincronización con Google Calendar
- Sistema de notificaciones por email
- Gestión de imágenes y archivos
- Sistema de comentarios y calificaciones
- Gestión de cupones de descuento
- Formularios de contacto

### 🛠️ **Tecnologías Utilizadas:**
- **Backend**: Spring Boot 3.2.3, Java 17
- **Base de Datos**: MySQL con JPA/Hibernate
- **Seguridad**: Spring Security + JWT + Google OAuth
- **Pagos**: Integración con MercadoPago
- **Email**: SendGrid para notificaciones
- **Calendario**: Google Calendar API
- **Documentación**: OpenAPI/Swagger
- **Build**: Maven

### 🏛️ **Patrones Arquitectónicos:**
- **MVC**: Separación clara de responsabilidades
- **Repository Pattern**: Abstracción de acceso a datos
- **Service Layer**: Lógica de negocio encapsulada
- **DTO Pattern**: Transferencia de datos optimizada
- **Builder Pattern**: Construcción de objetos complejos

## Notas Importantes

- Los diagramas utilizan la librería C4-PlantUML para mantener consistencia con el estándar C4
- Todos los diagramas están optimizados para ser legibles tanto en formato texto como visual
- Las relaciones muestran tanto el tipo de comunicación como el protocolo utilizado
- Los colores y estilos siguen las convenciones estándar de C4

## Contribuciones

Para modificar o actualizar los diagramas:
1. Edita el archivo `.puml` correspondiente
2. Verifica la sintaxis PlantUML
3. Genera una vista previa para confirmar que se ve correctamente
4. Actualiza esta documentación si es necesario
