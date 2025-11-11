# Modelo C4 - Sistema Agendalo

## Nivel 1: Diagrama de Contexto

```mermaid
graph TB
    subgraph "Usuarios Externos"
        U1[👤 Clientes<br/>Reservar citas]
        U2[👤 Propietarios<br/>Gestionar empresas]
        U3[👤 Administradores<br/>Gestionar sistema]
    end
    
    subgraph "Sistema Agendalo"
        SYS[🏢 Agendalo Platform<br/>Sistema de gestión de citas]
    end
    
    subgraph "Sistemas Externos"
        MP[💳 MercadoPago<br/>Procesamiento de pagos]
        GG[📧 Google Services<br/>OAuth + Calendar]
        SG[📨 SendGrid<br/>Envío de emails]
        DB[(🗄️ MySQL<br/>Base de datos)]
    end
    
    U1 -->|Reserva citas| SYS
    U2 -->|Gestiona empresa| SYS
    U3 -->|Administra sistema| SYS
    
    SYS -->|Procesa pagos| MP
    SYS -->|Autenticación| GG
    SYS -->|Sincroniza calendario| GG
    SYS -->|Envía notificaciones| SG
    SYS -->|Almacena datos| DB
    
    MP -->|Webhooks| SYS
    GG -->|Callbacks OAuth| SYS
```

## Nivel 2: Diagrama de Contenedores

```mermaid
graph TB
    subgraph "Usuarios"
        WEB[🌐 Frontend Web<br/>React/Vue.js]
        MOB[📱 Mobile App<br/>React Native/Flutter]
    end
    
    subgraph "Agendalo Backend"
        API[🔧 API Gateway<br/>Spring Boot REST API]
        AUTH[🔐 Auth Service<br/>JWT + OAuth]
        EMAIL[📧 Email Service<br/>SendGrid Integration]
        PAY[💳 Payment Service<br/>MercadoPago Integration]
        CAL[📅 Calendar Service<br/>Google Calendar API]
    end
    
    subgraph "Almacenamiento"
        DB[(🗄️ MySQL Database<br/>Datos principales)]
        FILES[📁 File Storage<br/>Imágenes y documentos]
    end
    
    subgraph "Servicios Externos"
        MP[💳 MercadoPago API]
        GG[📧 Google APIs]
        SG[📨 SendGrid API]
    end
    
    WEB -->|HTTPS/REST| API
    MOB -->|HTTPS/REST| API
    
    API --> AUTH
    API --> EMAIL
    API --> PAY
    API --> CAL
    
    AUTH -->|Autentica| GG
    EMAIL -->|Envía emails| SG
    PAY -->|Procesa pagos| MP
    CAL -->|Sincroniza| GG
    
    API -->|CRUD| DB
    API -->|Almacena| FILES
    
    MP -->|Webhooks| API
    GG -->|OAuth Callbacks| AUTH
```

## Nivel 3: Diagrama de Componentes - API Backend

```mermaid
graph TB
    subgraph "Agendalo Backend API"
        subgraph "Controllers Layer"
            UC[👤 UserController<br/>Gestión usuarios]
            CC[🏢 CompanyController<br/>Gestión empresas]
            AC[📅 AppointmentController<br/>Gestión citas]
            PC[💳 MercadoPagoController<br/>Gestión pagos]
            CALC[📅 GoogleCalendarController<br/>Sincronización]
            IC[🖼️ ImageController<br/>Gestión imágenes]
            COC[💬 CommentController<br/>Comentarios]
            COUC[🎫 CouponController<br/>Cupones]
            CONC[📞 ContactFormController<br/>Formularios]
        end
        
        subgraph "Services Layer"
            US[👤 UserService<br/>Lógica usuarios]
            CS[🏢 CompanyService<br/>Lógica empresas]
            AS[📅 AppointmentService<br/>Lógica citas]
            PS[💳 MercadoPagoService<br/>Lógica pagos]
            CALS[📅 GoogleCalendarService<br/>Lógica calendario]
            IS[🖼️ ImageService<br/>Lógica imágenes]
            COS[💬 CommentService<br/>Lógica comentarios]
            COUS[🎫 CouponService<br/>Lógica cupones]
            CONS[📞 ContactFormService<br/>Lógica formularios]
            ES[📧 EmailService<br/>Envío emails]
        end
        
        subgraph "Security & Config"
            SEC[🔐 SecurityConfig<br/>Configuración seguridad]
            JWT[🎫 JWTAuthFilter<br/>Filtro JWT]
            CORS[🌐 CorsConfig<br/>Configuración CORS]
            RATE[⏱️ RateLimiterConfig<br/>Rate limiting]
        end
        
        subgraph "Data Layer"
            UR[👤 UserRepository]
            CR[🏢 CompanyRepository]
            AR[📅 AppointmentRepository]
            PR[💳 PaymentPreferenceRepository]
            IR[🖼️ ImageRepository]
            COR[💬 CommentRepository]
            COUR[🎫 CouponRepository]
            CONR[📞 ContactFormRepository]
        end
    end
    
    subgraph "External Services"
        MP[💳 MercadoPago API]
        GG[📧 Google APIs]
        SG[📨 SendGrid API]
        DB[(🗄️ MySQL)]
    end
    
    UC --> US
    CC --> CS
    AC --> AS
    PC --> PS
    CALC --> CALS
    IC --> IS
    COC --> COS
    COUC --> COUS
    CONC --> CONS
    
    US --> UR
    CS --> CR
    AS --> AR
    PS --> PR
    IS --> IR
    COS --> COR
    COUS --> COUR
    CONS --> CONR
    
    US --> ES
    AS --> ES
    CS --> ES
    
    PS --> MP
    CALS --> GG
    ES --> SG
    
    UR --> DB
    CR --> DB
    AR --> DB
    PR --> DB
    IR --> DB
    COR --> DB
    COUR --> DB
    CONR --> DB
```

## Nivel 4: Diagrama de Código - Entidades de Dominio

```mermaid
erDiagram
    USER ||--o{ COMPANY : owns
    USER ||--o{ APPOINTMENT : books
    USER ||--o{ PROVIDER : is
    
    COMPANY ||--o{ BUSINESS_SERVICE : offers
    COMPANY ||--o{ BUSINESS_HOURS : has
    COMPANY ||--o{ APPOINTMENT : receives
    COMPANY ||--o{ IMAGE : has
    COMPANY ||--o{ PROVIDER : employs
    
    BUSINESS_SERVICE ||--o{ APPOINTMENT : booked_for
    BUSINESS_SERVICE ||--o{ PAYMENT_PREFERENCE : generates
    
    APPOINTMENT ||--|| PAYMENT_PREFERENCE : has
    APPOINTMENT ||--o{ COMMENT : has
    
    USER {
        Long id PK
        String email UK
        String password
        String phoneNumber
        String name
        String role
        String profileImageUrl
    }
    
    COMPANY {
        Long id PK
        Long owner_id FK
        String name
        String urlSlug UK
        String category
        String address
        Double latitude
        Double longitude
        String phone
        Integer minAdvanceDays
        Integer maxAdvanceDays
        Boolean hasTimeBetweenTurns
        Integer minutesBetweenTurns
        CompanyStatus status
        LocalDateTime registrationDate
        Integer cancellationHours
    }
    
    BUSINESS_SERVICE {
        Long id PK
        Long company_id FK
        String name
        String description
        Integer durationMinutes
        Double price
    }
    
    APPOINTMENT {
        Long id PK
        Long service_id FK
        Long user_id FK
        Long company_id FK
        LocalDateTime startTime
        LocalDateTime endTime
        AppointmentStatus status
        String notes
        String paymentId
        Double finalPrice
        String couponCode
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    PAYMENT_PREFERENCE {
        Long id PK
        String preferenceId UK
        Long service_id FK
        Long user_id FK
        Long company_id FK
        LocalDateTime startTime
        String notes
        Double amount
        String status
        String externalReference
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    BUSINESS_HOURS {
        Long id PK
        Long company_id FK
        DayOfWeek dayOfWeek
        LocalTime openingTime
        LocalTime closingTime
        Boolean workingDay
    }
    
    PROVIDER {
        Long id PK
        Long user_id FK
        Long company_id FK
    }
    
    IMAGE {
        Long id PK
        Long company_id FK
        String fileName
        String originalName
        String contentType
        Long size
        String url
        ImageType type
    }
    
    COMMENT {
        Long id PK
        Long appointment_id FK
        String content
        Integer rating
        LocalDateTime createdAt
    }
    
    COUPON {
        Long id PK
        String code UK
        String description
        Double discountPercentage
        Double discountAmount
        LocalDateTime validFrom
        LocalDateTime validUntil
        Integer maxUses
        Integer currentUses
        Boolean isActive
    }
    
    CONTACT_FORM {
        Long id PK
        String name
        String email
        String subject
        String message
        LocalDateTime createdAt
        Boolean isRead
    }
```

## Resumen de la Arquitectura

### Características Principales:
1. **Arquitectura REST**: API RESTful con Spring Boot
2. **Autenticación Multi-factor**: JWT + Google OAuth
3. **Integración de Pagos**: MercadoPago para procesamiento
4. **Sincronización de Calendario**: Google Calendar API
5. **Sistema de Notificaciones**: SendGrid para emails
6. **Gestión de Archivos**: Almacenamiento de imágenes
7. **Rate Limiting**: Protección contra abuso
8. **CORS**: Configuración para frontend

### Patrones de Diseño:
- **MVC**: Separación clara de responsabilidades
- **Repository Pattern**: Abstracción de acceso a datos
- **Service Layer**: Lógica de negocio encapsulada
- **DTO Pattern**: Transferencia de datos optimizada
- **Builder Pattern**: Construcción de objetos complejos

### Tecnologías Utilizadas:
- **Backend**: Spring Boot 3.2.3, Java 17
- **Base de Datos**: MySQL con JPA/Hibernate
- **Seguridad**: Spring Security + JWT
- **Documentación**: OpenAPI/Swagger
- **Testing**: JUnit + Spring Boot Test
- **Build**: Maven
