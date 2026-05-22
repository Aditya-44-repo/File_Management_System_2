# 📋 PROJECT ANALYSIS — DocIT File Management System

---

## 1. Project Summary

**DocIT** is a full-stack, enterprise-grade **File Load Management System** designed to handle CSV file ingestion, batch processing, lifecycle tracking, user management, and administrative oversight. It is built as a multi-module Maven monolith on the backend with a standalone Angular SPA on the frontend.

| Attribute         | Value                                              |
|-------------------|----------------------------------------------------|
| **App Name**      | DocIT                                              |
| **Type**          | Full-Stack Web Application                         |
| **Domain**        | File/Document Management & Processing              |
| **Frontend**      | Angular 17+ with Angular Material                  |
| **Backend**       | Spring Boot 3.3.8 (Java 21)                        |
| **Database**      | MySQL (via Spring Data JPA / Hibernate)            |
| **Auth**          | JWT (JJWT 0.12.6) + BCrypt + Token Versioning      |
| **Async**         | Spring Batch (asynchronous CSV processing)         |
| **API Docs**      | SpringDoc OpenAPI 2.6.0 (Swagger UI)               |
| **SSL**           | PKCS12 Keystore (HTTPS on 8080, HTTP on 8082)      |
| **Email**         | Spring Mail / Gmail SMTP (password reset)          |
| **Build Tool**    | Maven (multi-module POM)                           |

---

## 2. Tech Stack (Auto-Detected)

### Frontend
| Technology             | Purpose                                     |
|------------------------|---------------------------------------------|
| Angular 17+            | SPA framework                               |
| Angular Material       | UI component library (17+ modules)          |
| TypeScript             | Primary language                            |
| RxJS (BehaviorSubject) | Reactive state management                   |
| SCSS                   | Styling (3 partials: base, theme, overrides)|
| Angular Router         | Client-side navigation                      |
| HttpClient             | REST API communication                      |
| HTTP Interceptors      | Automatic JWT header injection              |
| localStorage           | Token/user session persistence              |

### Backend
| Technology               | Purpose                                     |
|--------------------------|---------------------------------------------|
| Spring Boot 3.3.8        | Application framework                       |
| Java 21                  | Runtime language                            |
| Spring Security          | Authentication, authorization, filters      |
| Spring Data JPA          | ORM / database abstraction                  |
| Spring Batch             | Asynchronous CSV file processing            |
| Spring Mail              | Email notifications (password reset)        |
| Hibernate (ddl-auto)     | Schema auto-update                          |
| MySQL                    | Relational database                         |
| JJWT 0.12.6              | JWT creation & validation                   |
| BCryptPasswordEncoder    | Password hashing                            |
| SpringDoc OpenAPI 2.6.0  | API documentation (Swagger UI)              |
| Maven Multi-Module       | Project build structure                     |

---

## 3. Folder / Module Structure

```
fileManagement-main/
├── README.md
├── backend/                          ← Maven parent POM (4 modules)
│   ├── pom.xml
│   ├── model/                        ← Shared data layer
│   │   └── src/main/java/com/fileload/model/
│   │       ├── entity/               ← JPA entities
│   │       └── dto/                  ← Data Transfer Objects
│   ├── dao/                          ← Database access layer
│   │   └── src/main/java/com/fileload/dao/
│   │       ├── repository/           ← Spring Data JPA repositories
│   │       └── specification/        ← JPA Specifications (dynamic queries)
│   ├── service/                      ← Business logic layer
│   │   └── src/main/java/com/fileload/service/
│   │       ├── impl/                 ← Service implementations
│   │       ├── batch/                ← Spring Batch job configs & tasklets
│   │       ├── mapper/               ← Entity-to-DTO mappers
│   │       └── util/                 ← Utilities (CSV record count)
│   └── api/                          ← REST API / Spring Boot entry point
│       └── src/main/java/com/fileload/api/
│           ├── FileLoadApiApplication.java  ← @SpringBootApplication
│           ├── controller/           ← REST controllers
│           ├── config/               ← Spring configuration beans
│           ├── security/             ← JWT filters, handlers, utilities
│           └── exception/            ← Global error handling
│       └── src/main/resources/
│           ├── application.yml       ← App configuration
│           └── keystore-local.pfx    ← SSL certificate
└── frontend/                         ← Angular standalone SPA
    └── src/
        ├── main.ts                   ← Angular bootstrap
        ├── index.html
        ├── styles.scss
        ├── environments/             ← API base URL config (dev/prod)
        ├── styles/                   ← _base.scss, _theme.scss, _overrides.scss
        └── app/
            ├── app.module.ts         ← Root Angular module
            ├── app-routing.module.ts ← Route definitions
            ├── app.component.ts      ← Root component
            ├── components/           ← 21 UI components
            ├── services/             ← 6 Angular services
            ├── models/               ← 4 TypeScript interfaces
            └── guards/               ← 3 route guards
```

---

## 4. Backend Module Breakdown

### 4.1 `model` — Shared Data Contracts

**Entities (JPA / Database tables):**

| Entity              | Table              | Purpose                                           |
|---------------------|--------------------|---------------------------------------------------|
| `UserAccount`       | `users`            | User accounts (credentials, role, lock state)     |
| `FileLoad`          | `file_load`        | Uploaded file records + processing state          |
| `AdminAuditEvent`   | `admin_audit_event`| Admin action history log                          |
| `PasswordResetToken`| `password_reset_token` | One-time tokens for password recovery         |
| `FileStatus`        | (enum)             | PENDING / PROCESSING / SUCCESS / FAILED / ARCHIVED|
| `UserRole`          | (enum)             | USER / ADMIN                                      |
| `AdminPermission`   | (enum)             | USER_ACCESS_CONTROL / USER_RECORDS_OVERVIEW / USER_FILES_DELETE_ALL |

**DTOs (Request/Response):**
- `LoginRequestDTO`, `RegisterRequestDTO`, `AuthResponseDTO`
- `FileLoadRequestDTO`, `FileLoadResponseDTO`
- `SearchCriteriaDTO`, `UpdateMetadataRequestDTO`, `UpdateStatusRequestDTO`
- `DashboardOverviewDTO`
- `ForgotPasswordRequestDTO`, `ResetPasswordRequestDTO`, `ResetPasswordResponseDTO`
- Admin DTOs: `AdminUserSummaryDTO`, `AdminAnalyticsDTO`, `AdminAuditEventDTO`, `BlockedIpRequestDTO`, `FeatureFlagUpdateRequestDTO`, etc.

### 4.2 `dao` — Data Access Layer

| Class                          | Purpose                                              |
|--------------------------------|------------------------------------------------------|
| `UserAccountRepository`        | CRUD + `findByEmail`, `existsByEmail/Username`, `findByEmailOrUsername` |
| `FileLoadRepository`           | CRUD + `countByStatus`, `countByUploadedById`, `findByUploadedById` |
| `AdminAuditEventRepository`    | Paginated audit log queries                          |
| `PasswordResetTokenRepository` | Token lookup and deletion                            |
| `FileLoadSpecifications`       | Dynamic JPA Specification for multi-criteria search  |

### 4.3 `service` — Business Logic

| Class                      | Purpose                                                       |
|----------------------------|---------------------------------------------------------------|
| `FileLoadServiceImpl`      | Upload validation, file storage, batch launch, search, download |
| `AdminServiceImpl`         | User management, analytics, audit log, force logout           |
| `PasswordResetService`     | Token generation, email dispatch, password reset              |
| `BatchConfig`              | Spring Batch `Job` and `Step` bean definitions                |
| `BatchJobLauncherService`  | Async launcher — fires batch job per file upload              |
| `FileLoadBatchEventListener`| Job lifecycle hooks (BEFORE/AFTER job execution)             |
| `FileLoadQueuedEvent`      | Custom Spring event for batch queuing                         |
| `FileProcessingTasklet`    | Core CSV analysis: PROCESSING → SUCCESS/FAILED                |
| `FileLoadMapper`           | Entity ↔ DTO mapping                                          |
| `RecordCountUtil`          | Counts CSV rows, detects format errors                        |

### 4.4 `api` — REST Layer / Entry Point

**Controllers:**

| Controller            | Base Path         | Roles Allowed             |
|-----------------------|-------------------|---------------------------|
| `AuthController`      | `/api/auth`       | Public + USER/ADMIN        |
| `FileLoadController`  | `/api/file-loads` | USER + ADMIN               |
| `AdminController`     | `/api/admin`      | ADMIN only (+ permissions) |

**Configuration:**

| Class                     | Purpose                                              |
|---------------------------|------------------------------------------------------|
| `SecurityConfig`          | Filter chain, CORS, session policy, route rules      |
| `AdminBootstrap`          | Creates default admin user on first run              |
| `AsyncConfig`             | Thread pool for async batch job execution            |
| `HttpAndHttpsConfig`      | Dual port — HTTPS:8080 + HTTP:8082                   |
| `OpenApiConfig`           | Swagger/OpenAPI metadata configuration               |
| `WebConfig`               | Static resource serving (`/uploads/**`)              |

**Security Filters:**

| Class                       | Purpose                                                |
|-----------------------------|--------------------------------------------------------|
| `JwtAuthenticationFilter`   | Validates JWT, checks token version, sets SecurityContext |
| `IpBlockFilter`             | Blocks requests from banned IPs (in-memory set)        |
| `JwtUtil`                   | JWT generation, validation, claim extraction           |
| `CustomUserDetailsService`  | Loads `UserDetails` from DB for Spring Security        |
| `SecurityControlService`    | In-memory IP blocklist + feature flags (ConcurrentHashMap) |
| `AdminAuthorizationService` | SpEL-based permission check (`@adminAuthorization.has(...)`) |
| `RestAuthenticationEntryPoint` | Returns 401 JSON on missing/invalid auth           |
| `RestAccessDeniedHandler`   | Returns 403 JSON on insufficient permission            |

---

## 5. Frontend Module Breakdown

### 5.1 Components (21 total)

| Component                  | Route                  | Purpose                                         |
|----------------------------|------------------------|-------------------------------------------------|
| `HomeComponent`            | `/home`                | Public landing page                             |
| `LoginComponent`           | `/login`               | Email/username + password login                 |
| `RegisterComponent`        | `/register`            | New user registration                           |
| `ForgotPasswordComponent`  | `/forgot-password`     | Request password reset email                    |
| `ResetPasswordComponent`   | `/reset-password`      | Token-based password reset                      |
| `OauthCallbackComponent`   | `/oauth/callback`      | OAuth2 redirect handler                         |
| `DashboardComponent`       | `/dashboard`           | Overview metrics + stats charts                 |
| `FileListComponent`        | `/files`               | Paginated file table with search/filter         |
| `FileUploadComponent`      | `/upload`              | Drag-and-drop CSV upload with progress bar      |
| `FileDetailsComponent`     | `/files/:id`           | File metadata + status + download               |
| `FileSearchComponent`      | (embedded)             | Reusable search/filter form                     |
| `StatusUpdateComponent`    | (dialog)               | Admin status change dialog                      |
| `AdminUsersComponent`      | `/admin/users`         | User management table (admin scope)             |
| `ProfileComponent`         | `/profile`             | User profile view/edit + avatar upload          |
| `ProfileDialogComponent`   | (dialog)               | Profile modal                                   |
| `NavbarComponent`          | (shared layout)        | Top navigation bar                              |
| `SidebarComponent`         | (shared layout)        | Side navigation menu                            |
| `FooterComponent`          | (shared layout)        | Footer                                          |
| `LayoutComponent`          | (wrapper)              | App shell layout                                |
| `ConfirmDialogComponent`   | (dialog)               | Generic confirm action modal                    |
| `UploadStatisticsDonutComponent` | (embedded)       | Donut chart for upload status distribution      |

### 5.2 Services (6 total)

| Service                   | Purpose                                                    |
|---------------------------|------------------------------------------------------------|
| `AuthService`             | Login, register, logout, profile, BehaviorSubject state    |
| `AuthInterceptor`         | Injects `Authorization: Bearer <token>` into all requests  |
| `FileLoadService`         | File CRUD, upload with progress, download, dashboard data  |
| `AdminService`            | User management API calls                                  |
| `FileUploadStatsService`  | Upload statistics aggregation                              |
| `PasswordResetService`    | Forgot/reset password API calls                            |

### 5.3 Guards (3 total)

| Guard              | Protects                          | Logic                                       |
|--------------------|-----------------------------------|---------------------------------------------|
| `AuthGuard`        | `/files`, `/upload`, `/profile`   | Checks `isAuthenticated()` via token        |
| `AdminGuard`       | Admin-only routes                 | Checks `role === 'ADMIN'`                   |
| `AdminScopeGuard`  | `/admin/users`                    | Checks specific admin permission flags      |

### 5.4 Models (TypeScript interfaces)

| Model                  | Fields                                               |
|------------------------|------------------------------------------------------|
| `User`                 | id, username, email, role, token, profileImage, adminPermissions |
| `FileItem`             | id, name, size, status, recordCount, tags, uploadedBy, errors |
| `SearchCriteria`       | fileId, filename, status, startDate, endDate, page, size, sort |
| `DashboardOverview`    | totalUploads, processingCount, successRate, failedToday |

---

## 6. Database Schema (Inferred from Entities)

### Table: `users`
| Column                  | Type          | Constraints                  |
|-------------------------|---------------|------------------------------|
| `id`                    | BIGINT        | PK, AUTO_INCREMENT           |
| `username`              | VARCHAR       | NOT NULL, UNIQUE             |
| `email`                 | VARCHAR       | NOT NULL, UNIQUE             |
| `password`              | VARCHAR       | NOT NULL (BCrypt)            |
| `role`                  | ENUM          | NOT NULL (USER/ADMIN)        |
| `profile_image`         | VARCHAR       | nullable                     |
| `failed_login_attempts` | INT           | NOT NULL, default 0          |
| `account_locked_until`  | DATETIME      | nullable                     |
| `enabled`               | BOOLEAN       | NOT NULL, default true       |
| `disabled_by_role`      | ENUM          | nullable                     |
| `token_version`         | INT           | NOT NULL, default 0          |
| `admin_permissions`     | VARCHAR(2000) | nullable (CSV of permissions)|

### Table: `file_load`
| Column          | Type          | Constraints                  |
|-----------------|---------------|------------------------------|
| `id`            | BIGINT        | PK, AUTO_INCREMENT           |
| `filename`      | VARCHAR       | NOT NULL                     |
| `file_type`     | VARCHAR       | NOT NULL                     |
| `file_size`     | BIGINT        | NOT NULL                     |
| `load_date`     | DATETIME      | NOT NULL                     |
| `status`        | ENUM          | NOT NULL                     |
| `record_count`  | BIGINT        | NOT NULL                     |
| `errors`        | LONGTEXT      | nullable                     |
| `description`   | VARCHAR(2000) | nullable                     |
| `tags`          | VARCHAR(2000) | nullable (CSV)               |
| `archived`      | BOOLEAN       | NOT NULL, default false      |
| `storage_path`  | VARCHAR       | NOT NULL (filesystem path)   |
| `uploaded_by_id`| BIGINT        | nullable (FK → users.id)     |
| `uploaded_by`   | VARCHAR(255)  | nullable (email string)      |

### Table: `admin_audit_event`
| Column       | Type      | Constraints           |
|--------------|-----------|-----------------------|
| `id`         | BIGINT    | PK                    |
| `action`     | VARCHAR   | e.g. "FILE_DELETED"   |
| `entity_type`| VARCHAR   | "FILE", "USER", "IP"  |
| `entity_id`  | VARCHAR   | Target entity ID      |
| `details`    | VARCHAR   | Context details       |
| `created_at` | DATETIME  | Timestamp             |

### Table: `password_reset_token`
| Column       | Type      | Constraints           |
|--------------|-----------|-----------------------|
| `id`         | BIGINT    | PK                    |
| `token`      | VARCHAR   | UNIQUE                |
| `email`      | VARCHAR   | User email            |
| `expires_at` | DATETIME  | Expiry timestamp      |

---

## 7. API Endpoints Summary

### Auth — `/api/auth`
| Method | Path                          | Auth    | Description                  |
|--------|-------------------------------|---------|------------------------------|
| POST   | `/register`                   | Public  | Register new user            |
| POST   | `/login`                      | Public  | Login, returns JWT           |
| POST   | `/forgot-password`            | Public  | Request password reset email |
| GET    | `/validate-reset-token/{token}` | Public| Validate reset token         |
| POST   | `/reset-password`             | Public  | Reset password with token    |
| GET    | `/profile`                    | Auth    | Get current user profile     |
| POST   | `/upload-profile`             | Auth    | Upload profile image         |

### File Loads — `/api/file-loads`
| Method | Path              | Auth        | Description                   |
|--------|-------------------|-------------|-------------------------------|
| POST   | `/`               | USER/ADMIN  | Upload CSV file               |
| GET    | `/{id}`           | USER/ADMIN  | Get file details              |
| GET    | `/`               | ADMIN       | Search all files (paginated)  |
| GET    | `/my`             | USER/ADMIN  | Search own files (paginated)  |
| GET    | `/overview`       | ADMIN       | Dashboard metrics             |
| PUT    | `/{id}/status`    | ADMIN       | Update file status            |
| PATCH  | `/{id}`           | USER/ADMIN  | Update file metadata          |
| DELETE | `/{id}`           | ADMIN       | Delete file                   |
| GET    | `/{id}/download`  | USER/ADMIN  | Download original file        |

### Admin — `/api/admin`
| Method | Path                               | Description                         |
|--------|------------------------------------|-------------------------------------|
| GET    | `/users`                           | List users (paginated)              |
| PATCH  | `/users/{id}/role`                 | Change user role                    |
| PATCH  | `/users/{id}/enabled`              | Enable/disable user                 |
| POST   | `/users/{id}/reset-failed-attempts`| Reset failed login counter          |
| POST   | `/users/{id}/force-logout`         | Invalidate all user JWTs            |
| GET    | `/users/{id}/file-count`           | Count user's files                  |
| DELETE | `/users/{id}/files`                | Delete all files by user            |
| PUT    | `/files/{id}/status`               | Admin file status update            |
| POST   | `/files/{id}/reprocess`            | Retry failed file processing        |
| DELETE | `/files/{id}`                      | Admin delete any file               |
| POST   | `/security/blocked-ips`            | Block IP address                    |
| DELETE | `/security/blocked-ips`            | Unblock IP address                  |
| GET    | `/security/blocked-ips`            | List blocked IPs                    |
| PUT    | `/feature-flags/{key}`             | Set feature flag                    |
| GET    | `/feature-flags`                   | List feature flags                  |
| GET    | `/analytics`                       | System-wide analytics               |
| GET    | `/audit-events`                    | Paginated admin audit log           |
| GET    | `/audit-events/export`             | Export audit log as CSV             |

---

## 8. Improvement Suggestions

### Architecture
1. **Add OAuth2 / Social Login fully** — `OauthCallbackComponent` exists but backend Google OAuth integration is incomplete (callback URLs configured, but no OAuth2 Spring Security provider found).
2. **Persistent IP Blocklist** — `SecurityControlService` stores blocked IPs in-memory (`ConcurrentHashMap`). Restarting the server clears all blocks. Persist to DB.
3. **Persistent Feature Flags** — Same issue as IP blocklist; stored in-memory, lost on restart.
4. **Decouple file storage** — Files saved to local filesystem (`uploads/`). Move to S3/Azure Blob/GCS for scalability.
5. **API Versioning** — No `/v1/` prefix on routes. Add versioning strategy for future-proofing.

### Security
6. **Token Blacklist on logout** — Currently logout only clears frontend localStorage. Backend tokens remain valid until expiry. Implement server-side blacklist or short expiry + refresh token.
7. **Rate Limiting** — No rate limiting on `/api/auth/login` (brute force risk). Add Spring Security or Bucket4J rate limiting.
8. **Profile image path traversal** — Upload path uses `file.getOriginalFilename()` directly. Sanitize thoroughly to prevent path traversal.
9. **CSRF disabled** — Acceptable for stateless JWT, but document explicitly.
10. **Admin permissions stored as CSV string** — `adminPermissions` column is a raw comma-separated string. Use a proper `@ElementCollection` join table.

### Performance
11. **N+1 Query Risk** — `FileLoad` does not use `@ManyToOne` join to `UserAccount` (stores `uploadedById` and `uploadedBy` as raw columns). This is intentional but prevents JPA joins.
12. **Batch delay is hardcoded** — `FileProcessingTasklet` sleeps 10 seconds to keep "PROCESSING" visible. This is a UI hack; use WebSocket or SSE for real-time status updates.
13. **No caching** — Add Spring Cache (`@Cacheable`) on dashboard overview and analytics endpoints.
14. **No database indexing defined** — Add `@Index` annotations on `file_load.status`, `file_load.uploaded_by_id`, `file_load.load_date`.

### Code Quality
15. **`AuthController` imports `UserAccountRepository` directly** — Bypasses the service layer. Move user-fetching logic to a `UserService`.
16. **Tags stored as CSV strings** — Both `FileLoad.tags` and `UserAccount.adminPermissions` use raw CSV strings. Use proper collections.
17. **Missing refresh token mechanism** — JWT expires in 24h with no refresh. Implement refresh token pattern.
18. **No unit/integration tests found** — Only a `test/` directory exists in `service`. Add comprehensive test coverage.

---

## 9. Potential Security Issues

| Risk                          | Severity | Location                                      |
|-------------------------------|----------|-----------------------------------------------|
| Path traversal on upload      | HIGH     | `AuthController.uploadProfile()` uses original filename |
| In-memory IP blocklist lost on restart | MEDIUM | `SecurityControlService`          |
| No login rate limiting         | HIGH     | `AuthController.login()`                      |
| No logout token invalidation   | MEDIUM   | `AuthService.logout()` — client-side only     |
| JWT secret in `.env` file      | MEDIUM   | Must not be committed; ensure `.gitignore`    |
| SMTP credentials in `.env`     | MEDIUM   | Exposed email password in README sample       |
| Admin permissions as plain CSV string | LOW | `UserAccount.adminPermissions` field         |
| No HTTPS enforcement redirect  | LOW      | HTTP port 8082 runs alongside HTTPS           |
| Feature flags not persisted    | LOW      | Lost on server restart                        |
