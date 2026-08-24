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
- `REDIS_HOST`, `REDIS_PORT` — Redis connection for OAuth `state` storage (`OAuthStateStore`); defaults point at `localhost:6379`. Like MySQL, `build`/`compileKotlin` work without a reachable Redis, but `bootRun` and context-loading tests need one.
- `JWT_SECRET` (min 256-bit; the raw string is hashed as-is, not base64-decoded — see `JwtTokenProvider`), `JWT_EXPIRATION_MS` — issuing app JWTs.
- `ADMIN_MASTER_CODE` — shared secret for the admin self-promotion endpoint.
- `FRONTEND_ORIGIN` — CORS allowed origin for the SPA (also used as the `Access-Control-Allow-Credentials` origin now that auth is cookie-based, see below).
- `SCHOOL_OAUTH_CLIENT_ID`, `SCHOOL_OAUTH_CLIENT_SECRET` — DataGSM OAuth client credentials (from `datagsm.kr/clients`), passed straight into `DataGsmOAuthClient`. No endpoint/claim-name config needed — the SDK already targets GSM's real SSO servers and returns typed models.
- `SCHOOL_OAUTH_REDIRECT_URI` — the backend's own DG callback URL (e.g. `http://localhost:8081/api/auth/dg/callback`); must exactly match what's registered as `redirect_uri` in the DG client console, since the backend now owns the redirect chain (see below).
- `APP_WEB_OAUTH_CALLBACK_REDIRECT_URL` — frontend URL the backend 302s to after login succeeds and the cookie is set. Confirmed 2026-08-24 (민욱): `https://2026-gsm-client.vercel.app/callback`. Blank means `GET /api/auth/dg/callback` fails fast with a 500 rather than silently redirecting nowhere — that fallback stays in place for any deploy that hasn't set this yet.
- `OAUTH_STATE_TTL_SECONDS` (optional, default `300`) — how long an `/authorize`-issued `state` stays valid in Redis before `/callback` must consume it.
- `OAUTH_COOKIE_SECURE` (optional, default `true`) — `Secure` flag on the `ACCESS_TOKEN` cookie. Browsers only send `Secure` cookies over HTTPS, so set this to `false` for local development over plain HTTP.

None of these have hardcoded fallback values in `application.yml` (except the two `OAUTH_*` ones noted above) — every one must be set in `.env` (see `.env.example`) or the environment, or the app fails to start.

## Architecture

### Package layout

`org.example.schoolweb` splits into:
- `domain/<feature>/{controller,dto,entity,repository,service}` — one package per business feature (currently `user`, `suggestion`). New features should follow this same per-feature layout rather than layering by type across the whole app.
- `global/config` — `@ConfigurationProperties` classes (auto-bound via `@ConfigurationPropertiesScan` in `SchoolwebApplication`, no explicit `@Bean` needed) plus `SecurityConfig`.
- `global/security` / `global/security/jwt` — JWT issuing/validation pipeline. No `global/security/oauth2` package anymore — school SSO is handled by `AuthService`/`OAuthController` (see below), not a Spring Security OAuth2 client filter chain.
- `global/exception` — `NotFoundException`/`ForbiddenException` + `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping to a uniform `ErrorResponse { status, message, fieldErrors }`.

### Auth flow (the non-obvious part)

The frontend is a separate-origin SPA, so auth is stateless JWT, not a server session. Login does **not** use Spring Security's `oauth2Login()`, but unlike an API-only integration, the **backend itself owns the DG redirect chain** (`OAuthController`, `/api/auth/dg/*`):

1. Frontend sends the browser to `GET /api/auth/dg/authorize` (`permitAll`, no fetch/JSON involved — a plain link/navigation). `AuthService.buildAuthorizationUrl` issues a random `state`, stores it in Redis via `OAuthStateStore` with a short TTL (`OAUTH_STATE_TTL_SECONDS`), and 302-redirects the browser to DG's authorization page (built with the SDK's `DataGsmOAuthClient.createAuthorizationUrl(...)`).
2. The user authenticates/consents on DG's own login page.
3. DG redirects the browser back to `GET /api/auth/dg/callback?code=...&state=...` (`permitAll`) — this URL must exactly match `SCHOOL_OAUTH_REDIRECT_URI` as registered in the DG client console.
4. `AuthService.completeSchoolOAuthLogin` first calls `OAuthStateStore.consume(state)`, which does a Redis `GETDEL` (atomic read-and-delete) so a `state` can only ever be used once — replay or a missing/expired `state` is rejected with `ForbiddenException`. It then uses `DataGsmOAuthClient` (bean wired in `OAuthClientConfig`, from the official `com.github.themoment-team:datagsm-oauth-sdk-java` SDK — see https://github.com/themoment-team/datagsm-oauth-sdk-java) to call `exchangeCodeForToken(code, redirectUri)` then `getUserInfo(accessToken)`. The SDK returns typed `UserInfo`/`Student` models, so there's no attribute-name mapping layer to maintain — non-student accounts (`AccountObjectType.TEACHER`) are rejected, and SDK errors (`DataGsmException`) are translated to `ForbiddenException`.
5. It upserts a `User` via `UserService.findOrCreateFromOAuth2`, keyed on **`UserInfo.id`, the top-level id from the `/userinfo` response — not `Student.id`** (they're different values; this was a real bug in an earlier iteration of this flow, worth double-checking if the DG userinfo response schema ever changes).
6. First-time DG login gets `Role.USER` immediately — no approval queue.
7. `OAuthController` sets the issued JWT as an `httpOnly` cookie (`ACCESS_TOKEN`; `Secure` per `OAUTH_COOKIE_SECURE`, `SameSite=None` since frontend/backend are different origins, `Max-Age` matching `JWT_EXPIRATION_MS`) and 302-redirects the browser to `APP_WEB_OAUTH_CALLBACK_REDIRECT_URL`. If that env var is blank (not yet finalized — never fill it in arbitrarily), the callback fails with a 500 instead of redirecting to nowhere.
8. `JwtAuthenticationFilter` reads the token from `Authorization: Bearer <token>` if present, otherwise falls back to the `ACCESS_TOKEN` cookie. Either way the JWT payload only carries the user id — role is re-looked-up from the DB per request, so an admin promotion (see below) takes effect immediately without needing to reissue a token.

Because auth now rides on a cookie the browser attaches automatically, CSRF is a live concern again (it wasn't under the old Bearer-only setup) — see the comment in `SecurityConfig` for why CSRF protection is still deliberately left disabled (every mutating endpoint requires a JSON body or a non-simple HTTP method, both of which need a CORS-preflight-passing origin) and what would break that assumption.

There is no `POST /api/auth/login` anymore — the old "frontend owns the redirect, backend just exchanges the code" flow (modeled on `team-incube/Flooding-Server-V2`) was replaced by the backend-owned flow described above.

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
