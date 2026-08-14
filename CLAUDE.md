# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Backend for a student council (학생회) web page. Spring Boot 4.1 + Kotlin 2.3, JPA/Hibernate over MySQL, stateless JWT auth backed by a school SSO OAuth2 login. Java 17 toolchain.

## Commands

Run from the project root (Git Bash: `./gradlew`, Windows cmd/PowerShell: `gradlew.bat`).

- Build: `./gradlew build`
- Run the app: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "org.example.schoolweb.SchoolwebApplicationTests"`
- Run a single test method: `./gradlew test --tests "org.example.schoolweb.SchoolwebApplicationTests.contextLoads"`

The app requires a MySQL instance and several env vars (see below); `./gradlew build`/`compileKotlin` work without them, but `bootRun` and integration tests that load the Spring context will fail to connect without a reachable DB.

### Required configuration (env vars, see `src/main/resources/application.yml`)

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` — MySQL connection (defaults point at `localhost:3306/schoolweb` with placeholder creds).
- `JWT_SECRET` (min 256-bit base64), `JWT_EXPIRATION_MS` — issuing app JWTs.
- `ADMIN_MASTER_CODE` — shared secret for the admin self-promotion endpoint.
- `FRONTEND_ORIGIN`, `FRONTEND_OAUTH2_REDIRECT_URI`, `FRONTEND_OAUTH2_ERROR_REDIRECT_URI` — CORS + where to redirect after OAuth2 login.
- `SCHOOL_OAUTH_CLIENT_ID`, `SCHOOL_OAUTH_CLIENT_SECRET`, `SCHOOL_OAUTH_AUTH_URI`, `SCHOOL_OAUTH_TOKEN_URI`, `SCHOOL_OAUTH_USERINFO_URI`, `SCHOOL_OAUTH_USERNAME_ATTRIBUTE`, `SCHOOL_OAUTH_ID_ATTR`, `SCHOOL_OAUTH_NAME_ATTR`, `SCHOOL_OAUTH_EMAIL_ATTR` — defaults now point at GSM's real SSO (`datagsm.kr`), with `id`/`student.name`/`email` as the userinfo claim names; override only for a different environment/tenant.

## Architecture

### Package layout

`org.example.schoolweb` splits into:
- `domain/<feature>/{controller,dto,entity,repository,service}` — one package per business feature (currently `user`, `suggestion`). New features should follow this same per-feature layout rather than layering by type across the whole app.
- `global/config` — `@ConfigurationProperties` classes (auto-bound via `@ConfigurationPropertiesScan` in `SchoolwebApplication`, no explicit `@Bean` needed) plus `SecurityConfig`.
- `global/security` / `global/security/jwt` / `global/security/oauth2` — auth pipeline.
- `global/exception` — `NotFoundException`/`ForbiddenException` + `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping to a uniform `ErrorResponse { status, message, fieldErrors }`.

### Auth flow (the non-obvious part)

The frontend is a separate-origin SPA, so auth is stateless JWT, not session cookies:

1. User hits the school's OAuth2 login (`/oauth2/**`, `/login/**` are `permitAll`; everything else requires authentication).
2. `CustomOAuth2UserService` calls `OAuth2AttributeExtractor` to pull the provider's user id/name/email out of the userinfo response, using attribute-name mappings from `OAuth2ExtractionProperties` (`app.oauth2.school.*`) — this indirection was built before GSM's real claim names were confirmed; the defaults now match GSM's SSO (`id`/`student.name`/`email`), but the properties stay in place so a different school tenant/environment only needs a config override, not a code change. It then upserts a `User` via `UserService.findOrCreateFromOAuth2`.
3. `OAuth2LoginSuccessHandler` issues a JWT (`JwtTokenProvider`) and redirects to `frontend.oauth2-redirect-uri` with the token in the URL **fragment** (`#token=...`), not a query param, so it never lands in server logs/Referer headers.
4. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>` on every request, but the JWT payload only carries the user id — role is re-looked-up from the DB per request. This is deliberate: it means an admin promotion (see below) takes effect immediately without needing to reissue a token.
5. GSM's SSO is confirmed to be plain OAuth2 (scope `datagsm:self_read`, no `openid` scope/id_token), so `CustomOAuth2UserService` is the right integration point — no `OidcUserService` migration needed.

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
