# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Backend for a student council (학생회) web page. Spring Boot 4.1 + Kotlin 2.3, JPA/Hibernate over MySQL, stateless JWT auth backed by a school SSO OAuth2 login via the official `datagsm-oauth-sdk-java` SDK. Java 17 toolchain.

## Commands

Run from the project root (Git Bash: `./gradlew`, Windows cmd/PowerShell: `gradlew.bat`).

- Build: `./gradlew build`
- Run the app: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "org.example.schoolweb.SchoolwebApplicationTests"`
- Run a single test method: `./gradlew test --tests "org.example.schoolweb.SchoolwebApplicationTests.contextLoads"`

The app requires a MySQL instance and several env vars (see below); `./gradlew build`/`compileKotlin` work without them, but `bootRun` and integration tests that load the Spring context will fail to connect without a reachable DB.

### Required configuration (env vars, see `src/main/resources/application.yml`)

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` — MySQL connection (defaults point at `localhost:3306/schoolweb`; username/password have no default and must be set).
- `JWT_SECRET` (min 256-bit; the raw string is hashed as-is, not base64-decoded — see `JwtTokenProvider`), `JWT_EXPIRATION_MS` — issuing app JWTs.
- `ADMIN_MASTER_CODE` — shared secret for the admin self-promotion endpoint.
- `FRONTEND_ORIGIN` — CORS allowed origin for the SPA.
- `SCHOOL_OAUTH_CLIENT_ID`, `SCHOOL_OAUTH_CLIENT_SECRET` — DataGSM OAuth client credentials (from `datagsm.kr/clients`), passed straight into `DataGsmOAuthClient`. No endpoint/claim-name config needed — the SDK already targets GSM's real SSO servers and returns typed models.

None of these have hardcoded fallback values in `application.yml` — every one must be set in `.env` (see `.env.example`) or the environment, or the app fails to start.

## Architecture

### Package layout

`org.example.schoolweb` splits into:
- `domain/<feature>/{controller,dto,entity,repository,service}` — one package per business feature (currently `user`, `suggestion`). New features should follow this same per-feature layout rather than layering by type across the whole app.
- `global/config` — `@ConfigurationProperties` classes (auto-bound via `@ConfigurationPropertiesScan` in `SchoolwebApplication`, no explicit `@Bean` needed) plus `SecurityConfig`.
- `global/security` / `global/security/jwt` — JWT issuing/validation pipeline. No `global/security/oauth2` package anymore — school SSO is handled by `AuthService` (see below), not a Spring Security OAuth2 client filter chain.
- `global/exception` — `NotFoundException`/`ForbiddenException` + `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping to a uniform `ErrorResponse { status, message, fieldErrors }`.

### Auth flow (the non-obvious part)

The frontend is a separate-origin SPA, so auth is stateless JWT, not session cookies. Login does **not** use Spring Security's `oauth2Login()` — it's a single unauthenticated API call:

1. The frontend itself redirects the user to DataGSM's SSO authorization page and receives `authCode` on its own callback route (the backend is never part of the redirect chain, unlike a typical Spring `oauth2Login()` setup).
2. Frontend calls `POST /api/auth/login` (`permitAll`) with `{ authCode, redirectUri }`.
3. `AuthService.loginWithSchoolOAuth` uses `DataGsmOAuthClient` (bean wired in `OAuthClientConfig`, from the official `com.github.themoment-team:datagsm-oauth-sdk-java` SDK — see https://github.com/themoment-team/datagsm-oauth-sdk-java) to call `exchangeCodeForToken(authCode, redirectUri)` then `getUserInfo(accessToken)`. The SDK returns typed `UserInfo`/`Student` models, so there's no attribute-name mapping layer to maintain (unlike a hand-rolled Spring OAuth2 client integration) — non-student accounts (`AccountObjectType.TEACHER`) are rejected, and SDK errors (`DataGsmException`) are translated to `ForbiddenException`.
4. It upserts a `User` via `UserService.findOrCreateFromOAuth2` (keyed on the SSO `Student.id`) and returns a JWT (`JwtTokenProvider`) as a plain JSON body (`LoginResponse`) — no redirect, no URL fragment, since this is a normal synchronous API call rather than a browser-redirect OAuth2 flow.
5. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>` on every subsequent request, but the JWT payload only carries the user id — role is re-looked-up from the DB per request. This is deliberate: it means an admin promotion (see below) takes effect immediately without needing to reissue a token.

Reference implementation this flow was modeled on: `team-incube/Flooding-Server-V2` (also GSM, same SDK, same "frontend owns the redirect, backend just exchanges the code" pattern).

### Roles & authorization

- Two roles only: `Role.USER`, `Role.ADMIN` (`domain/user/entity/Role.kt`).
- There's no admin-granting endpoint tied to identity — any authenticated user can self-promote via `POST /api/auth/promote` by supplying `ADMIN_MASTER_CODE` as a shared secret (`AuthController` / `AdminProperties`).
- Method-level authorization uses `@PreAuthorize("hasRole('ADMIN')")` (`@EnableMethodSecurity` in `SecurityConfig`); ownership checks that aren't role-based (e.g. "only the suggestion's author or an admin") are done manually in the service layer (`SuggestionService.checkAccess`), not via `@PreAuthorize`.

### Data/schema note

`spring.jpa.hibernate.ddl-auto` is `update` — schema is not yet stable. The TODO in `application.yml` flags switching to `validate` + Flyway/Liquibase once it settles; don't treat `ddl-auto: update` as the long-term migration strategy when adding entities.

## Git workflow

This repo follows Git Flow:

- `main` — always deployable; only receives merges from `release/*` or `hotfix/*` (or `develop` for early-stage/no-release-yet work).
- `develop` — integration branch; all `feature/*` branches merge back here.
- `feature/<short-description>` — one branch per feature/task, branched from `develop`, merged back into `develop` when done (e.g. `feature/init-project-setup`).
- `release/<version>` — cut from `develop` when preparing a release; merges into both `main` and `develop`.
- `hotfix/<short-description>` — cut from `main` for urgent production fixes; merges into both `main` and `develop`.

Do not commit directly to `main` or `develop` — always work on a `feature/*` (or `release/*`/`hotfix/*`) branch and merge in.
