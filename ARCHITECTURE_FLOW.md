# 🏗️ ARCHITECTURE FLOW — DocIT File Management System

---

## 1. High-Level System Architecture

```mermaid
graph TB
    subgraph CLIENT["🖥️ CLIENT LAYER — Angular SPA (Port 4200)"]
        direction TB
        BROWSER["Browser"]
        ANGULAR["Angular App\n(app.module.ts)"]
        ROUTES["Router\n(app-routing.module.ts)"]
        GUARDS["Route Guards\nAuthGuard | AdminGuard | AdminScopeGuard"]
        COMPONENTS["21 Components\n(UI Layer)"]
        SERVICES["6 Angular Services\n(AuthService, FileLoadService, AdminService...)"]
        INTERCEPTOR["AuthInterceptor\n(JWT Bearer token injection)"]
        STORE["Local State\nlocalStorage + BehaviorSubject"]
    end

    subgraph NETWORK["🌐 NETWORK LAYER"]
        HTTPS["HTTPS : 8080"]
        HTTP["HTTP : 8082"]
        CORS["CORS Policy\n(localhost origins only)"]
    end

    subgraph BACKEND["⚙️ BACKEND LAYER — Spring Boot 3.3.8 (Java 21)"]
        direction TB
        FILTERS["Security Filter Chain\nIpBlockFilter → JwtAuthFilter"]
        CONTROLLERS["REST Controllers\nAuthController | FileLoadController | AdminController"]
        SERVICES_BE["Business Services\nFileLoadService | AdminService | PasswordResetService"]
        BATCH["Spring Batch\nBatchConfig | BatchJobLauncherService | FileProcessingTasklet"]
        REPOSITORIES["Spring Data JPA Repositories\n(DAO Layer)"]
        SPECS["JPA Specifications\n(Dynamic Queries)"]
    end

    subgraph DATA["🗄️ DATA LAYER"]
        MYSQL[("MySQL Database\nfile_load_mgmt")]
        FS["Local Filesystem\n/uploads/ directory"]
    end

    subgraph EXTERNAL["☁️ EXTERNAL SERVICES"]
        SMTP["Gmail SMTP\n(Password Reset Emails)"]
        SWAGGER["Swagger UI\n/swagger-ui.html"]
        ACTUATOR["Spring Actuator\n/actuator/health"]
    end

    BROWSER --> ANGULAR
    ANGULAR --> ROUTES
    ROUTES --> GUARDS
    GUARDS --> COMPONENTS
    COMPONENTS --> SERVICES
    SERVICES --> INTERCEPTOR
    INTERCEPTOR -->|"HTTPS + JWT"| HTTPS
    INTERCEPTOR -->|"HTTP + JWT"| HTTP
    HTTPS --> CORS
    HTTP --> CORS
    CORS --> FILTERS
    FILTERS --> CONTROLLERS
    CONTROLLERS --> SERVICES_BE
    SERVICES_BE --> BATCH
    SERVICES_BE --> REPOSITORIES
    BATCH --> REPOSITORIES
    REPOSITORIES --> SPECS
    SPECS --> MYSQL
    REPOSITORIES --> MYSQL
    SERVICES_BE --> FS
    BATCH --> FS
    SERVICES_BE --> SMTP
    BACKEND --> SWAGGER
    BACKEND --> ACTUATOR
    SERVICES --> STORE
```

---

## 2. Frontend Architecture

```mermaid
graph TB
    subgraph BOOTSTRAP["Bootstrap"]
        MAIN["main.ts"]
        APP_MOD["AppModule\n(app.module.ts)"]
        APP_COMP["AppComponent\n(root)"]
    end

    subgraph ROUTING["Routing Layer"]
        ROUTER["Angular Router"]
        AG["AuthGuard\n→ /files, /upload, /profile"]
        ADG["AdminGuard\n→ ADMIN role check"]
        ADSG["AdminScopeGuard\n→ Permission check"]
    end

    subgraph PAGES["Page Components"]
        HOME["HomeComponent\n(/)"]
        LOGIN["LoginComponent\n(/login)"]
        REG["RegisterComponent\n(/register)"]
        FP["ForgotPasswordComponent\n(/forgot-password)"]
        RP["ResetPasswordComponent\n(/reset-password)"]
        OAUTH["OauthCallbackComponent\n(/oauth/callback)"]
        DASH["DashboardComponent\n(/dashboard)"]
        FILES["FileListComponent\n(/files)"]
        UPLOAD["FileUploadComponent\n(/upload)"]
        DETAILS["FileDetailsComponent\n(/files/:id)"]
        PROFILE["ProfileComponent\n(/profile)"]
        ADMIN_U["AdminUsersComponent\n(/admin/users)"]
    end

    subgraph SHARED["Shared Components"]
        NAVBAR["NavbarComponent"]
        SIDEBAR["SidebarComponent"]
        FOOTER["FooterComponent"]
        LAYOUT["LayoutComponent"]
        DONUT["UploadStatisticsDonutComponent"]
        FILESEARCH["FileSearchComponent"]
        STATUS["StatusUpdateComponent"]
        CONFIRM["ConfirmDialogComponent"]
        PROFDLG["ProfileDialogComponent"]
    end

    subgraph SERVICES["Angular Services"]
        AS["AuthService\n(BehaviorSubject state)"]
        FLS["FileLoadService\n(HTTP + progress events)"]
        ADMS["AdminService\n(User management)"]
        PRS["PasswordResetService"]
        FUSS["FileUploadStatsService"]
        INTER["AuthInterceptor\n(HTTP_INTERCEPTORS)"]
    end

    subgraph STATE["State Management"]
        BS["BehaviorSubject<User|null>"]
        LS["localStorage (fl_user key)"]
    end

    MAIN --> APP_MOD
    APP_MOD --> APP_COMP
    APP_COMP --> ROUTER
    ROUTER --> AG & ADG & ADSG
    AG --> FILES & UPLOAD & DETAILS & PROFILE
    ADSG --> ADMIN_U
    ROUTER --> HOME & LOGIN & REG & FP & RP & OAUTH & DASH

    LOGIN & REG --> AS
    DASH --> FLS & DONUT
    FILES --> FLS & FILESEARCH
    UPLOAD --> FLS
    DETAILS --> FLS & STATUS
    ADMIN_U --> ADMS
    PROFILE --> AS

    AS --> BS --> LS
    FLS --> INTER
    INTER -->|"Bearer Token"| HTTP_OUT["HttpClient → API"]
```

---

## 3. Backend Architecture (Layered)

```mermaid
graph LR
    subgraph API["api module — Spring Boot Entry"]
        direction TB
        MAIN_APP["FileLoadApiApplication\n@SpringBootApplication"]
        CFG["Config Package\nSecurityConfig | AsyncConfig\nWebConfig | OpenApiConfig\nHttpAndHttpsConfig | AdminBootstrap"]
        CTRL["Controller Package\nAuthController\nFileLoadController\nAdminController"]
        SEC["Security Package\nJwtUtil | JwtAuthFilter\nIpBlockFilter | SecurityControlService\nCustomUserDetailsService\nAdminAuthorizationService"]
        EXC["Exception Package\nGlobalExceptionHandler\nApiErrorResponse"]
    end

    subgraph SERVICE["service module — Business Logic"]
        direction TB
        SVC_IF["Service Interfaces\nFileLoadService\nAdminService\nPasswordResetService"]
        SVC_IMPL["Implementations\nFileLoadServiceImpl\nAdminServiceImpl"]
        BATCH_PKG["Batch Package\nBatchConfig | BatchJobLauncherService\nFileProcessingTasklet\nFileLoadBatchEventListener"]
        MAPPER["FileLoadMapper\n(Entity ↔ DTO)"]
        UTIL["RecordCountUtil\n(CSV analysis)"]
    end

    subgraph DAO["dao module — Data Access"]
        direction TB
        REPOS["Repositories\nUserAccountRepository\nFileLoadRepository\nPasswordResetTokenRepository\nAdminAuditEventRepository"]
        SPEC["FileLoadSpecifications\n(JPA Criteria API)"]
    end

    subgraph MODEL["model module — Shared Contracts"]
        direction TB
        ENTITIES["Entities\nUserAccount | FileLoad\nAdminAuditEvent\nPasswordResetToken"]
        DTOS["DTOs\nAuthResponseDTO | FileLoadResponseDTO\nDashboardOverviewDTO | SearchCriteriaDTO\n+ 15 more"]
        ENUMS["Enums\nUserRole | FileStatus\nAdminPermission"]
    end

    CTRL --> SVC_IF
    CTRL --> SEC
    SVC_IMPL --> REPOS
    SVC_IMPL --> MAPPER
    SVC_IMPL --> BATCH_PKG
    BATCH_PKG --> REPOS
    BATCH_PKG --> UTIL
    REPOS --> SPEC
    MAPPER --> ENTITIES
    MAPPER --> DTOS
    SVC_IF --> ENTITIES
```

---

## 4. Security & Authentication Architecture

```mermaid
graph TD
    subgraph FRONTEND_AUTH["Frontend Authentication"]
        UI_LOGIN["LoginComponent\nPOST /api/auth/login"]
        UI_REG["RegisterComponent\nPOST /api/auth/register"]
        UI_STORE["localStorage (fl_user)\n{id, username, email, role, token, adminPermissions}"]
        UI_BS["BehaviorSubject<User>\n(Reactive stream)"]
        UI_INT["AuthInterceptor\nClones request + adds Authorization: Bearer <token>"]
    end

    subgraph FILTER_CHAIN["Backend Filter Chain (per request)"]
        F1["① IpBlockFilter\nChecks ConcurrentHashMap of blocked IPs\n→ 403 if blocked"]
        F2["② JwtAuthenticationFilter\nExtracts Bearer token from header\nValidates signature\nChecks tokenVersion matches DB\nSets SecurityContext"]
        F3["③ Spring Security Authorization\nRoute-level: @PreAuthorize\nMethod-level: SpEL expressions"]
    end

    subgraph CONTROLLERS_AUTH["Controller Auth"]
        C1["AuthController\nGenerates JWT with email + tokenVersion + role"]
        C2["AdminController\n@PreAuthorize('hasRole(ADMIN)')\n@adminAuthorization.has(AdminPermission.X)"]
        C3["FileLoadController\n@PreAuthorize('hasAnyRole(USER,ADMIN)')"]
    end

    subgraph JWT_FLOW["JWT Internals"]
        JWT_GEN["JwtUtil.generateToken()\nSubject = email\nClaims = {tokenVersion, role}\nSigned with HMAC-SHA secret key"]
        JWT_VAL["JwtUtil.isTokenValid()\nJwtUtil.extractUsername()\nJwtUtil.extractTokenVersion()"]
        JWT_VER["Version Check\nDB user.tokenVersion == token.tokenVersion\nForce-logout invalidates by incrementing DB version"]
    end

    subgraph ADMIN_PERMS["Admin Permission System"]
        AP1["AdminPermission enum\nUSER_ACCESS_CONTROL\nUSER_RECORDS_OVERVIEW\nUSER_FILES_DELETE_ALL"]
        AP2["Stored as CSV in users.admin_permissions\n'USER_ACCESS_CONTROL,USER_RECORDS_OVERVIEW'"]
        AP3["AdminAuthorizationService\nSpEL: @adminAuthorization.has(AdminPermission.X)"]
        AP4["AdminScopeGuard (Angular)\nChecks same permissions on frontend"]
    end

    UI_LOGIN -->|"POST credentials"| C1
    UI_REG -->|"POST user data"| C1
    C1 -->|"Returns JWT"| UI_STORE
    UI_STORE --> UI_BS
    UI_BS --> UI_INT
    UI_INT -->|"Bearer token on every request"| F1
    F1 --> F2
    F2 -->|"Valid token"| F3
    F3 --> C2 & C3
    JWT_GEN --> JWT_VAL
    JWT_VAL --> JWT_VER
    C1 --> JWT_GEN
    AP1 --> AP2 --> AP3
    AP3 --> C2
    AP4 -.->|"Mirrors backend"| AP3
```

---

## 5. File Upload & Processing Architecture

```mermaid
graph TD
    subgraph FRONTEND_UPLOAD["Frontend Upload Flow"]
        FU_UI["FileUploadComponent\nDrag & drop / file picker"]
        FU_SVC["FileLoadService.upload()\nHttpRequest with reportProgress: true"]
        FU_PROG["Progress Bar\nHttpEventType.UploadProgress events"]
    end

    subgraph BACKEND_UPLOAD["Backend Upload Flow"]
        API_RECV["FileLoadController.createFileLoad()\nPOST /api/file-loads\n(multipart/form-data)"]
        VALIDATION["FileLoadServiceImpl.createFileLoad()\n① Check .csv extension\n② Check file not empty\n③ Check size ≤ 20MB"]
        SAVE_FILE["Save to /uploads/ directory\nresolvеUniquePath() for collision handling"]
        PERSIST["fileLoadRepository.saveAndFlush()\nStatus = PENDING\nRecordCount = 0"]
        LAUNCH["BatchJobLauncherService.launch(fileLoadId)\n@Async — fires on background thread"]
    end

    subgraph BATCH_PROC["Spring Batch Processing (Async)"]
        JOB["fileProcessingJob\n(Spring Batch Job)"]
        STEP["processFileStep\n(Spring Batch Step)"]
        TASKLET["FileProcessingTasklet.execute()\n① Set status → PROCESSING (DB)\n② Sleep 10s (UI visibility)\n③ RecordCountUtil.analyzeFile()\n④ Set status → SUCCESS or FAILED"]
        RCU["RecordCountUtil\nReads CSV file from filesystem\nCounts rows, detects errors"]
    end

    subgraph STATUS_LIFECYCLE["FileStatus Lifecycle"]
        S1["PENDING\n(just uploaded)"]
        S2["PROCESSING\n(batch running)"]
        S3["SUCCESS\n(CSV valid, row count set)"]
        S4["FAILED\n(validation error / exception)"]
        S5["ARCHIVED\n(soft-archived)"]
        S1 --> S2 --> S3
        S2 --> S4
        S3 -->|"Admin action"| S5
        S4 -->|"Admin reprocess"| S1
    end

    FU_UI --> FU_SVC
    FU_SVC --> FU_PROG
    FU_SVC -->|"Multipart POST"| API_RECV
    API_RECV --> VALIDATION
    VALIDATION -->|"Invalid"| PERSIST_FAIL["Persist FAILED record\n(no file saved)"]
    VALIDATION -->|"Valid"| SAVE_FILE
    SAVE_FILE --> PERSIST
    PERSIST --> LAUNCH
    LAUNCH --> JOB
    JOB --> STEP
    STEP --> TASKLET
    TASKLET --> RCU
```

---

## 6. Admin Architecture

```mermaid
graph LR
    subgraph ADMIN_FRONTEND["Admin Frontend"]
        AU["AdminUsersComponent\n/admin/users"]
        AS_SVC["AdminService\n(Angular)"]
        DASH2["DashboardComponent\n(Admin metrics)"]
        FLS2["FileLoadService\n(Admin file ops)"]
    end

    subgraph ADMIN_GUARDS["Frontend Guards"]
        ADSG2["AdminScopeGuard\nChecks adminPermissions"]
        ADGG["AdminGuard\nChecks role===ADMIN"]
    end

    subgraph ADMIN_BACKEND["Admin Backend"]
        AC["AdminController\n/api/admin\n@PreAuthorize('hasRole(ADMIN)')"]
        ADMSVC["AdminService (backend)\nAdminServiceImpl"]
        SCS["SecurityControlService\nIP blocklist + Feature flags"]
        AAS["AdminAuthorizationService\nSpEL @adminAuthorization.has(...)"]
    end

    subgraph ADMIN_OPS["Admin Operations"]
        OP1["User Management\nList | Enable/Disable | Role Change\nReset attempts | Force logout"]
        OP2["File Operations\nStatus update | Reprocess | Delete any file\nDelete all user files"]
        OP3["Security Controls\nBlock/Unblock IPs\nFeature flags toggle"]
        OP4["Analytics & Audit\nSystem analytics\nAudit event log (paginated)\nExport audit as CSV"]
    end

    subgraph AUDIT["Audit Trail"]
        AUD["AdminAuditEvent\naction | entityType | entityId | details | timestamp"]
        AUDREP["AdminAuditEventRepository"]
    end

    ADSG2 --> AU
    AU --> AS_SVC
    DASH2 --> FLS2
    AS_SVC -->|"HTTP"| AC
    FLS2 -->|"HTTP"| AC
    AC --> ADMSVC & SCS
    AC --> AAS
    ADMSVC --> OP1 & OP2 & OP4
    SCS --> OP3
    ADMSVC --> AUDREP --> AUD
```

---

## 7. Password Reset Flow

```mermaid
sequenceDiagram
    participant U as User (Browser)
    participant FE as Angular Frontend
    participant BE as Spring Boot API
    participant DB as MySQL
    participant SMTP as Gmail SMTP

    U->>FE: Clicks "Forgot Password"
    FE->>FE: Navigate to /forgot-password
    U->>FE: Enters email
    FE->>BE: POST /api/auth/forgot-password {email}
    BE->>DB: Find UserAccount by email
    alt User not found
        BE-->>FE: 404 "Email not registered"
    else User found
        BE->>BE: Generate UUID token
        BE->>DB: Save PasswordResetToken {token, email, expiresAt}
        BE->>SMTP: Send email with reset link
        BE-->>FE: 200 "Reset link sent"
    end
    FE-->>U: Show success message

    U->>FE: Clicks reset link in email
    FE->>FE: Navigate to /reset-password?token=...
    FE->>BE: GET /api/auth/validate-reset-token/{token}
    alt Token invalid/expired
        BE-->>FE: 400 "Invalid or expired token"
    else Valid token
        BE-->>FE: 200 "Valid token"
        U->>FE: Enters new password
        FE->>BE: POST /api/auth/reset-password {token, newPassword}
        BE->>DB: Find token, verify not expired
        BE->>DB: Update UserAccount.password (BCrypt)
        BE->>DB: Increment tokenVersion (invalidates all JWTs)
        BE->>DB: Delete PasswordResetToken
        BE-->>FE: 200 "Password reset successful"
        FE->>FE: Navigate to /login
    end
```

---

## 8. Complete Request-Response Lifecycle

```mermaid
sequenceDiagram
    participant B as Browser
    participant ANG as Angular App
    participant INT as AuthInterceptor
    participant BACK as Spring Boot
    participant IPFILT as IpBlockFilter
    participant JWTFILT as JwtAuthFilter
    participant SEC as Spring Security
    participant CTRL as Controller
    participant SVC as Service
    participant REPO as Repository
    participant DB as MySQL

    B->>ANG: User action (click/submit)
    ANG->>INT: HttpClient request
    INT->>INT: Reads token from AuthService
    INT->>BACK: HTTP request + "Authorization: Bearer <jwt>"

    BACK->>IPFILT: Request enters filter chain
    IPFILT->>IPFILT: Check IP against blockedIps set
    alt IP is blocked
        IPFILT-->>B: 403 Forbidden
    end

    IPFILT->>JWTFILT: Pass to JWT filter
    JWTFILT->>JWTFILT: Extract Bearer token
    JWTFILT->>JWTFILT: JwtUtil.isTokenValid(token)
    JWTFILT->>DB: Find UserAccount by email
    JWTFILT->>JWTFILT: Compare token.tokenVersion == user.tokenVersion
    alt Token invalid or version mismatch
        JWTFILT->>SEC: No authentication set
        SEC-->>B: 401 Unauthorized (RestAuthenticationEntryPoint)
    end

    JWTFILT->>SEC: Set SecurityContext with UserDetails + roles
    SEC->>CTRL: Check @PreAuthorize / route rules
    alt Insufficient role
        SEC-->>B: 403 Forbidden (RestAccessDeniedHandler)
    end

    CTRL->>SVC: Call business method
    SVC->>REPO: Database query
    REPO->>DB: SQL (JPA/Hibernate)
    DB-->>REPO: Result set
    REPO-->>SVC: Entity
    SVC->>SVC: Map Entity → DTO
    SVC-->>CTRL: ResponseDTO
    CTRL-->>B: HTTP 200 + JSON body
    ANG->>ANG: Update component state
    B->>B: Re-render UI
```

---

## 9. Data Flow Diagram

```mermaid
flowchart LR
    subgraph USER_IN["User Input"]
        UI1["Login form"]
        UI2["File picker"]
        UI3["Search filters"]
        UI4["Admin actions"]
    end

    subgraph FE_TRANSFORM["Frontend Transform"]
        TS1["AuthService.login()"]
        TS2["FileLoadService.upload()"]
        TS3["FileLoadService.list()"]
        TS4["AdminService.*()"]
    end

    subgraph HTTP_LAYER["HTTP Transport"]
        H1["POST /auth/login → {login, password}"]
        H2["POST /file-loads → FormData(file)"]
        H3["GET /file-loads/my → ?page=0&size=10"]
        H4["PATCH /admin/users/{id}/enabled"]
    end

    subgraph BE_PROCESS["Backend Processing"]
        P1["Authenticate → Generate JWT"]
        P2["Validate → Save file → Queue batch"]
        P3["JPA Specification query → Page<FileLoad>"]
        P4["Update DB → Record audit event"]
    end

    subgraph STORAGE["Storage"]
        DB2[("MySQL")]
        FS2["Filesystem /uploads/"]
    end

    subgraph RESPONSE["Response"]
        R1["AuthResponseDTO {token, role, permissions}"]
        R2["FileLoadResponseDTO {id, status=PENDING}"]
        R3["Page<FileLoadResponseDTO>"]
        R4["AdminUserSummaryDTO"]
    end

    UI1-->TS1-->H1-->P1-->DB2-->R1-->FE_TRANSFORM
    UI2-->TS2-->H2-->P2-->DB2 & FS2-->R2-->FE_TRANSFORM
    UI3-->TS3-->H3-->P3-->DB2-->R3-->FE_TRANSFORM
    UI4-->TS4-->H4-->P4-->DB2-->R4-->FE_TRANSFORM
```

---

## 10. Dependency Map

```mermaid
graph TD
    API_MOD["api module"]
    SVC_MOD["service module"]
    DAO_MOD["dao module"]
    MODEL_MOD["model module"]
    SPRING_BOOT["spring-boot-starter-parent\n3.3.8"]
    SPRING_BATCH["spring-batch"]
    SPRING_SEC["spring-security"]
    SPRING_JPA["spring-data-jpa"]
    SPRING_MAIL["spring-mail"]
    MYSQL_DRV["mysql-connector-j"]
    JJWT["jjwt 0.12.6\n(api + impl + jackson)"]
    SPRINGDOC["springdoc-openapi 2.6.0"]
    LOMBOK["Not used"]
    ANGULAR["Angular 17+"]
    NG_MAT["Angular Material\n(17 modules)"]
    RXJS["RxJS\n(BehaviorSubject, Observable)"]
    TS["TypeScript"]

    API_MOD --> SVC_MOD
    API_MOD --> DAO_MOD
    API_MOD --> MODEL_MOD
    SVC_MOD --> DAO_MOD
    SVC_MOD --> MODEL_MOD
    DAO_MOD --> MODEL_MOD

    API_MOD --> SPRING_BOOT & SPRING_SEC & SPRINGDOC & JJWT & SPRING_MAIL
    SVC_MOD --> SPRING_BOOT & SPRING_BATCH
    DAO_MOD --> SPRING_JPA & MYSQL_DRV
    MODEL_MOD --> SPRING_JPA

    ANGULAR --> NG_MAT & RXJS & TS
```
