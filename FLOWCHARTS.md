# 📊 FLOWCHARTS — DocIT File Management System

---

## 1. Complete Application Execution Flow

> **From user action → backend → database → response → UI update**

```mermaid
flowchart TD
    START(["👤 User Opens Browser\nhttps://localhost:4200"])

    START --> ROUTE_CHK{"Route\nGuard Check"}
    ROUTE_CHK -->|"Public route\n(home/login/register)"| PUBLIC_PAGE["Render Public Page"]
    ROUTE_CHK -->|"Protected route\n(AuthGuard)"| AUTH_CHK{"isAuthenticated?\n(token in localStorage)"}
    AUTH_CHK -->|"No"| REDIRECT_LOGIN["→ Navigate to /login"]
    AUTH_CHK -->|"Yes"| RENDER_PAGE["Render Protected Page"]

    PUBLIC_PAGE --> USER_ACTION["User Performs Action\n(login / register / upload / search)"]
    RENDER_PAGE --> USER_ACTION
    REDIRECT_LOGIN --> USER_ACTION

    USER_ACTION --> FE_SVC["Angular Service\ncalled with payload"]
    FE_SVC --> INTERCEPTOR["AuthInterceptor\nClones HttpRequest +\nAdds Authorization: Bearer {token}"]
    INTERCEPTOR --> HTTP_CALL["HttpClient sends request\nto https://localhost:8080/api/..."]

    HTTP_CALL --> IP_FILTER["IpBlockFilter\nChecks IP against blocked set"]
    IP_FILTER -->|"IP blocked"| ERR_403["HTTP 403 → UI shows error"]
    IP_FILTER -->|"IP allowed"| JWT_FILTER["JwtAuthenticationFilter\nExtracts + validates JWT"]

    JWT_FILTER --> TOKEN_CHK{"Token\nvalid?"}
    TOKEN_CHK -->|"Invalid/expired"| ERR_401["HTTP 401 → AuthService.logout() → /login"]
    TOKEN_CHK -->|"Valid"| VERSION_CHK{"tokenVersion\nmatch?"}
    VERSION_CHK -->|"Mismatch\n(force-logged out)"| ERR_401
    VERSION_CHK -->|"Match"| SECURITY_CTX["Set SecurityContext\nwith UserDetails + ROLE"]

    SECURITY_CTX --> PREAUTH{"@PreAuthorize\ncheck passes?"}
    PREAUTH -->|"No"| ERR_403
    PREAUTH -->|"Yes"| CONTROLLER["Controller Method\nexecutes"]

    CONTROLLER --> SERVICE["Service Layer\nbusiness logic"]
    SERVICE --> REPO["Repository\n(Spring Data JPA)"]
    REPO --> DB[("MySQL\nfile_load_mgmt")]
    DB --> REPO
    REPO --> SERVICE

    SERVICE -->|"File upload path"| BATCH_LAUNCH["BatchJobLauncherService\n@Async launch"]
    BATCH_LAUNCH --> BATCH_JOB["Spring Batch Job\n(background thread)"]
    BATCH_JOB --> CSV_PROC["FileProcessingTasklet\nPENDING → PROCESSING → SUCCESS/FAILED"]

    SERVICE --> DTO["Map Entity → DTO\nFileLoadMapper"]
    DTO --> CONTROLLER
    CONTROLLER --> HTTP_RESP["HTTP Response\nJSON body + status code"]

    HTTP_RESP --> FE_SVC
    FE_SVC --> RX_PIPE["RxJS Observable\n.pipe(map, tap)"]
    RX_PIPE --> COMP_STATE["Component updates\nlocal state / BehaviorSubject"]
    COMP_STATE --> UI_RENDER["Angular re-renders\ntemplate binding"]
    UI_RENDER --> END_USER["✅ User sees updated UI"]
```

---

## 2. User Registration & Login Flowchart

```mermaid
flowchart TD
    subgraph REGISTER["Registration Flow"]
        R1(["User at /register"])
        R2["Fill username, email, password"]
        R3["Submit form"]
        R4{"Email already\nexists?"}
        R5{"Username already\nexists?"}
        R6["Save UserAccount\nrole=USER, enabled=true"]
        R7["Generate JWT\n{email, tokenVersion=0, role=USER}"]
        R8["Return AuthResponseDTO"]
        R9["Angular: localStorage.setItem('fl_user', ...)"]
        R10["BehaviorSubject.next(user)"]
        R11(["Navigate to /dashboard"])

        R1-->R2-->R3-->R4
        R4-->|"Yes"| ERR_R1["Error: Email exists"]
        R4-->|"No"| R5
        R5-->|"Yes"| ERR_R2["Error: Username exists"]
        R5-->|"No"| R6-->R7-->R8-->R9-->R10-->R11
    end

    subgraph LOGIN["Login Flow"]
        L1(["User at /login"])
        L2["Enter email/username + password"]
        L3["POST /api/auth/login"]
        L4{"User found\nin DB?"}
        L5{"User\nenabled?"}
        L6["Spring Security\nAuthenticationManager.authenticate()"]
        L7{"Credentials\nvalid?"}
        L8["Generate JWT\n{email, tokenVersion, role}"]
        L9["Return AuthResponseDTO"]
        L10["Angular: save to localStorage"]
        L11(["Navigate to /dashboard"])

        L1-->L2-->L3-->L4
        L4-->|"No"| ERR_L1["Error: Invalid credentials"]
        L4-->|"Yes"| L5
        L5-->|"No"| ERR_L2["Error: User is blocked"]
        L5-->|"Yes"| L6-->L7
        L7-->|"Wrong password"| ERR_L3["Error: Invalid credentials"]
        L7-->|"Valid"| L8-->L9-->L10-->L11
    end
```

---

## 3. File Upload Flowchart

```mermaid
flowchart TD
    U1(["User at /upload"])
    U2["Selects CSV file\n(drag & drop or file picker)"]
    U3["Optionally adds\ndescription + tags"]
    U4["Clicks Upload"]
    U5["FileLoadService.upload(file, extras)\nPOST /api/file-loads (multipart)"]
    U6["Progress bar updates\nHttpEventType.UploadProgress"]

    U7{"File extension\n=== .csv?"}
    U8{"File is\nempty?"}
    U9{"File size\n> 20MB?"}

    U10["Save file to /uploads/\nresolvеUniquePath() for collisions"]
    U11["Create FileLoad entity\nstatus=PENDING, recordCount=0"]
    U12["fileLoadRepository.saveAndFlush()"]
    U13["BatchJobLauncherService.launch(fileLoadId)\n@Async — returns immediately"]
    U14["Return FileLoadResponseDTO\nstatus=PENDING"]

    U15["UI shows success toast\nFile appears in list as PENDING"]

    B1["Create FAILED FileLoad record\n(no file saved to disk)"]
    B2["Return FileLoadResponseDTO\nstatus=FAILED with error"]
    B3["UI shows error in file list"]

    BATCH1["Background Batch Job\nfileProcessingJob"]
    BATCH2["processFileStep →\nFileProcessingTasklet"]
    BATCH3["Update status → PROCESSING\n(DB + saveAndFlush)"]
    BATCH4["Thread.sleep(10_000)\n(keep PROCESSING visible in UI)"]
    BATCH5["RecordCountUtil.analyzeFile(path)"]
    BATCH6{"Errors\nfound?"}
    BATCH7["Update status → SUCCESS\nSet recordCount"]
    BATCH8["Update status → FAILED\nSet error message"]

    U1-->U2-->U3-->U4-->U5-->U6
    U5-->U7
    U7-->|"No"| B1-->B2-->B3
    U7-->|"Yes"| U8
    U8-->|"Yes"| B1
    U8-->|"No"| U9
    U9-->|"Yes"| B1
    U9-->|"No"| U10-->U11-->U12-->U13-->U14-->U15

    U13-->BATCH1-->BATCH2-->BATCH3-->BATCH4-->BATCH5-->BATCH6
    BATCH6-->|"No"| BATCH7
    BATCH6-->|"Yes"| BATCH8
```

---

## 4. Admin Operations Flowchart

```mermaid
flowchart TD
    A0(["Admin navigates to /admin/users"])
    A1{"AdminScopeGuard:\nhas USER_ACCESS_CONTROL or\nUSER_RECORDS_OVERVIEW or\nUSER_FILES_DELETE_ALL?"}
    A1-->|"No"| REDIR["→ /dashboard"]
    A1-->|"Yes"| A2["AdminUsersComponent loads\nGET /api/admin/users"]

    A2 --> A3["Display user table\n(paginated + searchable)"]
    A3 --> ADMIN_ACTION{"Admin selects action"}

    ADMIN_ACTION -->|"Block/Unblock user"| BU1["PATCH /admin/users/{id}/enabled\n{enabled: false/true}"]
    ADMIN_ACTION -->|"Change role"| CR1["PATCH /admin/users/{id}/role\n{role: 'ADMIN'/'USER'}"]
    ADMIN_ACTION -->|"Force logout"| FL1["POST /admin/users/{id}/force-logout"]
    ADMIN_ACTION -->|"Reset failed attempts"| RF1["POST /admin/users/{id}/reset-failed-attempts"]
    ADMIN_ACTION -->|"Delete all user files"| DF1["DELETE /admin/users/{id}/files"]
    ADMIN_ACTION -->|"View file count"| FC1["GET /admin/users/{id}/file-count"]

    BU1 --> AUD1["AdminServiceImpl.audit()\n→ Save AdminAuditEvent to DB"]
    CR1 --> AUD1
    FL1 --> FL2["Increment user.tokenVersion\n→ All existing JWTs invalidated"]
    FL2 --> AUD1
    DF1 --> DF2["Delete files from filesystem\n+ Delete DB records"]
    DF2 --> AUD1

    AUD1 --> RESP["Return updated AdminUserSummaryDTO"]
    RESP --> UI_UPD["Table row updates in real-time"]

    subgraph SECURITY_OPS["Security Operations"]
        SO1["POST /admin/security/blocked-ips\n→ SecurityControlService.blockIp()"]
        SO2["DELETE /admin/security/blocked-ips\n→ SecurityControlService.unblockIp()"]
        SO3["PUT /admin/feature-flags/{key}\n→ SecurityControlService.setFeatureFlag()"]
        SO1 & SO2 & SO3 --> AUD1
    end

    subgraph ANALYTICS["Analytics & Audit"]
        AN1["GET /admin/analytics\n→ AdminService.getAnalytics()"]
        AN2["GET /admin/audit-events\n→ Paginated audit log"]
        AN3["GET /admin/audit-events/export\n→ Download CSV"]
    end
```

---

## 5. JWT Token Lifecycle Flowchart

```mermaid
flowchart LR
    CREATE(["User\nRegisters/Logs in"])
    CREATE --> GEN["JwtUtil.generateToken()\nPayload: {sub: email,\ntokenVersion: int,\nrole: string}\nSigned: HMAC-SHA256"]
    GEN --> STORE["Stored in\nlocalStorage (frontend)"]
    STORE --> USE["Every HTTP request:\nAuthorization: Bearer <token>"]
    USE --> VALIDATE{"JwtUtil.isTokenValid()"}
    VALIDATE -->|"Invalid signature\nor expired"| REJ["Reject request\n401 Unauthorized"]
    VALIDATE -->|"Valid"| VERSION{"DB\ntokenVersion\n== token\ntokenVersion?"}
    VERSION -->|"Mismatch"| REJ
    VERSION -->|"Match"| GRANT["Request proceeds\nSecurityContext set"]

    GRANT --> EXPIRE{"Token\nexpired?\n(24h default)"}
    EXPIRE -->|"Yes"| LOGOUT["Frontend detects 401\nAuthService.logout()\nClear localStorage\nNavigate /login"]
    EXPIRE -->|"No"| GRANT

    INVALIDATE["Admin: Force Logout\nPOST /admin/users/{id}/force-logout"]
    INVALIDATE --> INC["user.tokenVersion++\n(DB update)"]
    INC --> VERSION
```

---

## 6. Error Handling Flow

```mermaid
flowchart TD
    ERR_SRC["Error Source"]

    ERR_SRC --> BE_ERR["Backend Exception"]
    ERR_SRC --> FE_ERR["Frontend Error"]
    ERR_SRC --> NET_ERR["Network Error"]

    BE_ERR --> GEH["GlobalExceptionHandler\n@RestControllerAdvice"]
    GEH --> EXC_TYPE{"Exception Type"}

    EXC_TYPE -->|"IllegalArgumentException"| E400["HTTP 400\nApiErrorResponse {status, message, timestamp}"]
    EXC_TYPE -->|"EntityNotFoundException"| E404["HTTP 404\nApiErrorResponse"]
    EXC_TYPE -->|"AccessDeniedException"| E403["HTTP 403\nRestAccessDeniedHandler"]
    EXC_TYPE -->|"AuthenticationException"| E401["HTTP 401\nRestAuthenticationEntryPoint"]
    EXC_TYPE -->|"MaxUploadSizeExceededException"| E413["HTTP 413\nFile too large"]
    EXC_TYPE -->|"Unhandled Exception"| E500["HTTP 500\nApiErrorResponse"]

    FE_ERR --> FE_HANDLER["RxJS catchError()"]
    NET_ERR --> FE_HANDLER
    E400 & E404 & E403 & E401 & E413 & E500 --> FE_HANDLER

    FE_HANDLER --> FE_RESPONSE{"Status Code"}
    FE_RESPONSE -->|"401"| FORCE_LOGOUT["AuthService.logout()\nNavigate to /login"]
    FE_RESPONSE -->|"403"| SHOW_DENIED["MatSnackBar: Access Denied"]
    FE_RESPONSE -->|"400 / 404 / 500"| SHOW_ERR["MatSnackBar: Error message"]

    BATCH_ERR["Batch Processing\nException"] --> TASKLET_CATCH["FileProcessingTasklet\ntry/catch"]
    TASKLET_CATCH --> DB_FAIL["Update FileLoad\nstatus=FAILED\nerrors=exception message"]
    DB_FAIL --> UI_POLL["UI reflects FAILED\nstatus on next load"]
```

---

## 7. Angular Routing & Navigation Flowchart

```mermaid
flowchart TD
    ENTRY(["Browser navigates to URL"])
    ROUTER["Angular Router\n(app-routing.module.ts)"]

    ENTRY --> ROUTER

    ROUTER --> ROUTE_MATCH{"Route Match"}

    ROUTE_MATCH --> PUB_ROUTES["Public Routes\n(no guards)"]
    ROUTE_MATCH --> AUTH_ROUTES["Protected Routes\ncanActivate: [AuthGuard]"]
    ROUTE_MATCH --> ADMIN_ROUTES["Admin Routes\ncanActivate: [AdminScopeGuard]"]
    ROUTE_MATCH --> WILDCARD["** → /home"]

    PUB_ROUTES --> PR1["/ → HomeComponent"]
    PUB_ROUTES --> PR2["/login → LoginComponent"]
    PUB_ROUTES --> PR3["/register → RegisterComponent"]
    PUB_ROUTES --> PR4["/forgot-password → ForgotPasswordComponent"]
    PUB_ROUTES --> PR5["/reset-password → ResetPasswordComponent"]
    PUB_ROUTES --> PR6["/oauth/callback → OauthCallbackComponent"]

    AUTH_ROUTES --> AG_CHK{"AuthGuard:\ntoken in\nlocalStorage?"}
    AG_CHK -->|"No"| NAV_LOGIN["Navigate to /login\n+ returnUrl queryParam"]
    AG_CHK -->|"Yes"| AUTH_COMP["Render Component"]

    AUTH_COMP --> AC1["/dashboard → DashboardComponent"]
    AUTH_COMP --> AC2["/files → FileListComponent"]
    AUTH_COMP --> AC3["/files/:id → FileDetailsComponent"]
    AUTH_COMP --> AC4["/upload → FileUploadComponent"]
    AUTH_COMP --> AC5["/profile → ProfileComponent"]

    ADMIN_ROUTES --> ADSG_CHK{"AdminScopeGuard:\nhasAnyAdminPermission?\n(USER_ACCESS_CONTROL\n| USER_RECORDS_OVERVIEW\n| USER_FILES_DELETE_ALL)"}
    ADSG_CHK -->|"No"| NAV_DASH["Navigate to /dashboard"]
    ADSG_CHK -->|"Yes"| AD1["/admin/users → AdminUsersComponent"]

    subgraph LAYOUT["Shared Layout (always rendered)"]
        NAV["NavbarComponent"]
        SIDE["SidebarComponent"]
        FOOT["FooterComponent"]
    end
```

---

## 8. Module Interaction Chart

```mermaid
graph TB
    subgraph ANGULAR_APP["Angular Application"]
        APP_MOD2["AppModule"]
        AUTH_SVC2["AuthService"]
        FILE_SVC2["FileLoadService"]
        ADMIN_SVC2["AdminService"]
        INTER2["AuthInterceptor"]
        GUARDS2["Guards (3)"]
        COMPS2["Components (21)"]
        MODELS2["Models (4 interfaces)"]
    end

    subgraph SPRING_API["Spring Boot API Layer"]
        AUTH_CTRL2["AuthController"]
        FILE_CTRL2["FileLoadController"]
        ADMIN_CTRL2["AdminController"]
        SEC_CFG2["SecurityConfig"]
        JWT_FILT2["JwtAuthenticationFilter"]
        IP_FILT2["IpBlockFilter"]
        EXC_HDL2["GlobalExceptionHandler"]
    end

    subgraph SPRING_SVC["Service Layer"]
        FILE_SVC_BE2["FileLoadServiceImpl"]
        ADMIN_SVC_BE2["AdminServiceImpl"]
        PWD_SVC2["PasswordResetService"]
        BATCH_SVC2["BatchJobLauncherService"]
        BATCH_TASK2["FileProcessingTasklet"]
        MAPPER2["FileLoadMapper"]
    end

    subgraph SPRING_DAO["DAO Layer"]
        USER_REPO2["UserAccountRepository"]
        FILE_REPO2["FileLoadRepository"]
        TOKEN_REPO2["PasswordResetTokenRepository"]
        AUDIT_REPO2["AdminAuditEventRepository"]
        SPEC2["FileLoadSpecifications"]
    end

    subgraph SPRING_MODEL["Model Layer"]
        ENTITIES2["JPA Entities (7)"]
        DTOS2["DTOs (22)"]
        ENUMS2["Enums (3)"]
    end

    subgraph EXTERNAL_SERVICES["External Services"]
        MYSQL2[("MySQL DB")]
        FS3["Filesystem /uploads/"]
        SMTP2["Gmail SMTP"]
    end

    APP_MOD2 --> AUTH_SVC2 & FILE_SVC2 & ADMIN_SVC2 & INTER2 & GUARDS2 & COMPS2
    AUTH_SVC2 & FILE_SVC2 & ADMIN_SVC2 --> INTER2
    INTER2 -->|"HTTPS"| AUTH_CTRL2 & FILE_CTRL2 & ADMIN_CTRL2
    SEC_CFG2 --> JWT_FILT2 & IP_FILT2
    AUTH_CTRL2 --> FILE_SVC_BE2 & PWD_SVC2
    FILE_CTRL2 --> FILE_SVC_BE2
    ADMIN_CTRL2 --> ADMIN_SVC_BE2 & FILE_SVC_BE2
    FILE_SVC_BE2 --> FILE_REPO2 & USER_REPO2 & BATCH_SVC2 & MAPPER2
    ADMIN_SVC_BE2 --> USER_REPO2 & AUDIT_REPO2 & FILE_REPO2
    PWD_SVC2 --> TOKEN_REPO2 & USER_REPO2 & SMTP2
    BATCH_SVC2 --> BATCH_TASK2
    BATCH_TASK2 --> FILE_REPO2 & FS3
    FILE_REPO2 & USER_REPO2 & TOKEN_REPO2 & AUDIT_REPO2 --> SPEC2
    FILE_REPO2 & USER_REPO2 --> MYSQL2
    MAPPER2 --> DTOS2 & ENTITIES2
    ENTITIES2 --> MYSQL2
    FILE_SVC_BE2 --> FS3
```

---

## 9. Async/Background Processing Flow

```mermaid
sequenceDiagram
    participant USER as User (Browser)
    participant CTRL as FileLoadController
    participant SVC as FileLoadServiceImpl
    participant REPO as FileLoadRepository
    participant DB as MySQL
    participant LAUNCHER as BatchJobLauncherService (@Async)
    participant JOB as Spring Batch Job
    participant TASKLET as FileProcessingTasklet
    participant FS as Filesystem (/uploads/)

    USER->>CTRL: POST /api/file-loads (CSV file)
    CTRL->>SVC: createFileLoad(file)
    SVC->>SVC: Validate extension, size, emptiness
    SVC->>FS: Save file to /uploads/filename.csv
    SVC->>REPO: saveAndFlush(entity {status=PENDING})
    REPO->>DB: INSERT INTO file_load (status='PENDING')
    DB-->>REPO: entity with id=42
    REPO-->>SVC: saved entity
    SVC->>LAUNCHER: launch(fileLoadId=42) — @Async returns immediately
    SVC-->>CTRL: FileLoadResponseDTO {status=PENDING}
    CTRL-->>USER: HTTP 201 {status: "PENDING"}

    Note over LAUNCHER,TASKLET: Background thread pool (AsyncConfig)
    LAUNCHER->>JOB: JobLauncher.run(fileProcessingJob, params={fileLoadId=42})
    JOB->>TASKLET: execute(StepContribution, ChunkContext)
    TASKLET->>DB: UPDATE file_load SET status='PROCESSING' WHERE id=42
    TASKLET->>TASKLET: Thread.sleep(10_000ms)
    TASKLET->>FS: Read /uploads/filename.csv
    TASKLET->>TASKLET: RecordCountUtil.analyzeFile(path)
    alt CSV valid
        TASKLET->>DB: UPDATE file_load SET status='SUCCESS', record_count=N WHERE id=42
    else CSV invalid / exception
        TASKLET->>DB: UPDATE file_load SET status='FAILED', errors='...' WHERE id=42
    end

    Note over USER,DB: User polls /api/file-loads/my to see updated status
    USER->>CTRL: GET /api/file-loads/my
    CTRL->>SVC: searchMyFileLoads(criteria)
    SVC->>REPO: findAll(spec, pageable)
    REPO->>DB: SELECT * FROM file_load WHERE uploaded_by_id=?
    DB-->>USER: [{id:42, status:"SUCCESS", recordCount:150}]
```

---

## 10. Folder Structure — Visual Map

```mermaid
mindmap
  root((DocIT))
    backend
      pom.xml
        parent: spring-boot-starter-parent 3.3.8
        modules: model, dao, service, api
      model
        entity
          UserAccount ← users table
          FileLoad ← file_load table
          AdminAuditEvent
          PasswordResetToken
          FileStatus enum
          UserRole enum
          AdminPermission enum
        dto
          Auth: Login, Register, AuthResponse
          File: FileLoadRequest, FileLoadResponse
          Admin: 10 admin DTOs
          Password: Forgot, Reset, Response
      dao
        repository
          UserAccountRepository
          FileLoadRepository
          PasswordResetTokenRepository
          AdminAuditEventRepository
        specification
          FileLoadSpecifications
      service
        impl
          FileLoadServiceImpl
          AdminServiceImpl
        batch
          BatchConfig
          BatchJobLauncherService
          FileProcessingTasklet
          FileLoadBatchEventListener
          FileLoadQueuedEvent
        mapper
          FileLoadMapper
        util
          RecordCountUtil
      api
        FileLoadApiApplication
        controller
          AuthController
          FileLoadController
          AdminController
        config
          SecurityConfig
          AsyncConfig
          WebConfig
          OpenApiConfig
          HttpAndHttpsConfig
          AdminBootstrap
        security
          JwtUtil
          JwtAuthenticationFilter
          IpBlockFilter
          CustomUserDetailsService
          SecurityControlService
          AdminAuthorizationService
          RestAuthenticationEntryPoint
          RestAccessDeniedHandler
        exception
          GlobalExceptionHandler
          ApiErrorResponse
        resources
          application.yml
          keystore-local.pfx
    frontend
      src
        main.ts
        index.html
        styles.scss
        environments
          environment.ts
          environment.prod.ts
        styles
          _base.scss
          _theme.scss
          _overrides.scss
        app
          app.module.ts
          app-routing.module.ts
          app.component.ts
          components (21)
            Home, Login, Register
            ForgotPassword, ResetPassword
            OauthCallback
            Dashboard
            FileList, FileUpload, FileDetails
            FileSearch, StatusUpdate
            AdminUsers
            Profile, ProfileDialog
            Navbar, Sidebar, Footer, Layout
            ConfirmDialog
            UploadStatisticsDonut
          services (6)
            AuthService
            AuthInterceptor
            FileLoadService
            AdminService
            FileUploadStatsService
            PasswordResetService
          models (4)
            User
            FileItem
            SearchCriteria
            DashboardOverview
          guards (3)
            AuthGuard
            AdminGuard
            AdminScopeGuard
```
